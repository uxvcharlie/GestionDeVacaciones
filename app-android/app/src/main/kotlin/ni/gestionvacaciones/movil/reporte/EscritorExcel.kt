package ni.gestionvacaciones.movil.reporte

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escribe tablas como un archivo Excel (.xlsx), una hoja por tabla.
 *
 * Un .xlsx es un archivo .zip con varios XML adentro (norma Office Open XML).
 * Lo armamos a mano, sin bibliotecas: es poco código y evita sumarle a la app
 * una dependencia pesada que además no siempre funciona en Android.
 *
 * - Los textos se guardan como texto, nunca como fórmulas: no hay riesgo de
 *   inyección al abrirlo.
 * - Las fechas se guardan como fechas reales con formato dd/mm/aaaa, para que
 *   se puedan ordenar y filtrar.
 * - La fila de encabezados va en negrita y queda fija al bajar.
 */
object EscritorExcel {

    const val TIPO_CONTENIDO = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private const val ANCHO_MINIMO = 10
    private const val ANCHO_MAXIMO = 60

    // Índices de estilo definidos en styles.xml
    private const val ESTILO_NEGRITA = 1
    private const val ESTILO_FECHA = 2

    /** Excel cuenta las fechas como días desde el 30/12/1899. */
    private val ORIGEN_EXCEL: LocalDate = LocalDate.of(1899, 12, 30)

    fun escribir(tablas: List<Tabla>): ByteArray {
        require(tablas.isNotEmpty()) { "Un libro de Excel necesita al menos una hoja." }
        val salida = ByteArrayOutputStream()
        ZipOutputStream(salida).use { zip ->
            fun parte(nombre: String, xml: String) {
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            parte("[Content_Types].xml", tiposDeContenido(tablas.size))
            parte("_rels/.rels", RELACIONES_RAIZ)
            parte("xl/workbook.xml", libro(tablas))
            parte("xl/_rels/workbook.xml.rels", relacionesDelLibro(tablas.size))
            parte("xl/styles.xml", ESTILOS)
            tablas.forEachIndexed { i, tabla -> parte("xl/worksheets/sheet${i + 1}.xml", hoja(tabla)) }
        }
        return salida.toByteArray()
    }

    // -------------------------------------------------------------------------

    private fun hoja(tabla: Tabla): String {
        val columnas = tabla.encabezados.size
        val anchos = IntArray(columnas) { tabla.encabezados[it].length }
        val datos = StringBuilder()

        datos.append("<row r=\"1\">")
        tabla.encabezados.forEachIndexed { c, encabezado ->
            datos.append(celdaTexto(referencia(c, 1), encabezado, ESTILO_NEGRITA))
        }
        datos.append("</row>")

        tabla.filas.forEachIndexed { f, fila ->
            val renglon = f + 2
            datos.append("<row r=\"$renglon\">")
            for (c in 0 until minOf(columnas, fila.size)) {
                val valor = fila[c] ?: continue
                val ref = referencia(c, renglon)
                when (valor) {
                    is Int, is Long -> {
                        datos.append("<c r=\"$ref\"><v>$valor</v></c>")
                        anchos[c] = maxOf(anchos[c], valor.toString().length)
                    }
                    is LocalDate -> {
                        val serie = ChronoUnit.DAYS.between(ORIGEN_EXCEL, valor)
                        datos.append("<c r=\"$ref\" s=\"$ESTILO_FECHA\"><v>$serie</v></c>")
                        anchos[c] = maxOf(anchos[c], 10)
                    }
                    else -> {
                        val texto = valor.toString()
                        datos.append(celdaTexto(ref, texto, 0))
                        anchos[c] = maxOf(anchos[c], texto.length)
                    }
                }
            }
            datos.append("</row>")
        }

        val cols = StringBuilder("<cols>")
        anchos.forEachIndexed { i, ancho ->
            val final = (ancho + 2).coerceIn(ANCHO_MINIMO, ANCHO_MAXIMO)
            cols.append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$final\" customWidth=\"1\"/>")
        }
        cols.append("</cols>")

        return XML_DECL +
            "<worksheet xmlns=\"$NS_MAIN\">" +
            "<sheetViews><sheetView workbookViewId=\"0\">" +
            "<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>" +
            "</sheetView></sheetViews>" +
            (if (columnas > 0) cols.toString() else "") +
            "<sheetData>$datos</sheetData>" +
            "</worksheet>"
    }

    private fun celdaTexto(ref: String, texto: String, estilo: Int): String {
        val atributoEstilo = if (estilo > 0) " s=\"$estilo\"" else ""
        val preservar = if (texto.isNotEmpty() && (texto.first().isWhitespace() || texto.last().isWhitespace())) {
            " xml:space=\"preserve\""
        } else ""
        return "<c r=\"$ref\" t=\"inlineStr\"$atributoEstilo><is><t$preservar>${escapar(texto)}</t></is></c>"
    }

    /** 0 → A, 25 → Z, 26 → AA ... */
    fun columna(indice: Int): String {
        var n = indice + 1
        val letras = StringBuilder()
        while (n > 0) {
            val resto = (n - 1) % 26
            letras.insert(0, ('A' + resto))
            n = (n - 1) / 26
        }
        return letras.toString()
    }

    private fun referencia(columna: Int, fila: Int) = columna(columna) + fila

    /** Escapa los caracteres especiales de XML y quita los que XML no admite. */
    fun escapar(texto: String): String {
        val salida = StringBuilder(texto.length)
        for (ch in texto) {
            when {
                ch == '&' -> salida.append("&amp;")
                ch == '<' -> salida.append("&lt;")
                ch == '>' -> salida.append("&gt;")
                ch == '"' -> salida.append("&quot;")
                ch == '\t' || ch == '\n' || ch == '\r' -> salida.append(ch)
                ch < ' ' -> Unit // caracteres de control: XML no los admite
                else -> salida.append(ch)
            }
        }
        return salida.toString()
    }

    /** Excel exige nombres de hoja de hasta 31 caracteres y sin []:*?/\ */
    fun nombreDeHoja(nombre: String): String {
        val limpio = nombre.replace(Regex("[\\[\\]:*?/\\\\]"), " ").trim().take(31)
        return limpio.ifEmpty { "Hoja" }
    }

    private fun libro(tablas: List<Tabla>): String {
        val hojas = tablas.mapIndexed { i, t ->
            "<sheet name=\"${escapar(nombreDeHoja(t.nombre))}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
        }.joinToString("")
        return XML_DECL + "<workbook xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_REL_DOC\"><sheets>$hojas</sheets></workbook>"
    }

    private fun relacionesDelLibro(cantidadHojas: Int): String {
        val hojas = (1..cantidadHojas).joinToString("") {
            "<Relationship Id=\"rId$it\" Type=\"$NS_REL_DOC/worksheet\" Target=\"worksheets/sheet$it.xml\"/>"
        }
        val estilos = "<Relationship Id=\"rId${cantidadHojas + 1}\" Type=\"$NS_REL_DOC/styles\" Target=\"styles.xml\"/>"
        return XML_DECL + "<Relationships xmlns=\"$NS_REL_PKG\">$hojas$estilos</Relationships>"
    }

    private fun tiposDeContenido(cantidadHojas: Int): String {
        val hojas = (1..cantidadHojas).joinToString("") {
            "<Override PartName=\"/xl/worksheets/sheet$it.xml\" " +
                "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
        }
        return XML_DECL +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\" " +
            "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\" " +
            "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
            hojas +
            "</Types>"
    }

    private const val XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL_DOC = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_REL_PKG = "http://schemas.openxmlformats.org/package/2006/relationships"

    private const val RELACIONES_RAIZ = XML_DECL +
        "<Relationships xmlns=\"$NS_REL_PKG\">" +
        "<Relationship Id=\"rId1\" Type=\"$NS_REL_DOC/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    // Estilo 0: normal · 1: negrita · 2: fecha dd/mm/yyyy
    private const val ESTILOS = XML_DECL +
        "<styleSheet xmlns=\"$NS_MAIN\">" +
        "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\"/></numFmts>" +
        "<fonts count=\"2\">" +
        "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
        "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
        "</fonts>" +
        "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>" +
        "<fill><patternFill patternType=\"gray125\"/></fill></fills>" +
        "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"3\">" +
        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
        "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "</cellXfs>" +
        "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
        "</styleSheet>"
}
