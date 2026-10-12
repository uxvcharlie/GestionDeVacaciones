package ni.gestionvacaciones.movil.reporte

import ni.gestionvacaciones.movil.dominio.Fechas
import java.time.LocalDate

/**
 * Escribe una tabla como CSV que Excel abre bien con tildes y eñes.
 * Copia fiel de `EscritorCsv.java` de la web.
 *
 * - UTF-8 con BOM al principio: sin esa marca, Excel muestra "VÃ¡squez".
 * - Separado por comas y con comillas donde hace falta (norma RFC 4180).
 * - **Protección contra inyección de fórmulas:** si un texto empieza con =, +,
 *   -, @, tabulador o retorno, se le antepone un apóstrofo. Si no, un motivo
 *   escrito como "=HIPERVINCULO(...)" se ejecutaría al abrir el archivo.
 */
object EscritorCsv {

    private const val MARCA_UTF8 = "﻿"

    fun escribir(tabla: Tabla): ByteArray {
        val texto = StringBuilder(MARCA_UTF8)
        linea(texto, tabla.encabezados)
        tabla.filas.forEach { linea(texto, it) }
        return texto.toString().toByteArray(Charsets.UTF_8)
    }

    private fun linea(texto: StringBuilder, celdas: List<Any?>) {
        celdas.forEachIndexed { i, valor ->
            if (i > 0) texto.append(',')
            texto.append(celda(valor))
        }
        texto.append("\r\n")
    }

    fun celda(valor: Any?): String {
        if (valor == null) return ""
        if (valor is Number) return valor.toString()
        if (valor is LocalDate) return Fechas.fecha(valor)
        val texto = neutralizarFormula(valor.toString())
        val necesitaComillas = texto.contains(',') || texto.contains('"') || texto.contains('\n') ||
            texto.contains('\r') || texto.startsWith(" ") || texto.endsWith(" ")
        return if (necesitaComillas) "\"" + texto.replace("\"", "\"\"") + "\"" else texto
    }

    private fun neutralizarFormula(texto: String): String =
        if (texto.isNotEmpty() && texto[0] in "=+-@\t\r") "'$texto" else texto
}
