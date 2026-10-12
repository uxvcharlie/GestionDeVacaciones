package ni.gestionvacaciones.movil.reporte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class EscritoresTest {

    private val tabla = Tabla(
        "Solicitudes",
        listOf("Funcionario", "Desde", "Minutos", "Motivo"),
        listOf(
            listOf("María González", LocalDate.of(2026, 9, 21), 480, "=HIPERVINCULO(\"malo\")"),
            listOf("Ángela Pérez, Núñez", LocalDate.of(2026, 9, 22), 120, null),
        ),
    )

    @Test
    fun `CSV con BOM, comillas y formulas neutralizadas`() {
        val texto = String(EscritorCsv.escribir(tabla), Charsets.UTF_8)
        assertTrue("empieza con la marca UTF-8", texto.startsWith("﻿"))
        val lineas = texto.removePrefix("﻿").split("\r\n")
        assertEquals("Funcionario,Desde,Minutos,Motivo", lineas[0])
        assertEquals("María González,21/09/2026,480,\"'=HIPERVINCULO(\"\"malo\"\")\"", lineas[1])
        assertEquals("\"Ángela Pérez, Núñez\",22/09/2026,120,", lineas[2])
    }

    @Test
    fun `celdas CSV que empiezan con signos peligrosos llevan apostrofo`() {
        listOf("=1+1", "+1", "-1", "@SUMA").forEach {
            assertTrue(it, EscritorCsv.celda(it).startsWith("'"))
        }
        assertEquals("-5", EscritorCsv.celda(-5)) // un número negativo NO es texto peligroso
    }

    @Test
    fun `el xlsx es un zip con todas sus partes y XML bien formado`() {
        val bytes = EscritorExcel.escribir(listOf(tabla, Tabla("Resumen", listOf("Dato", "Valor"), emptyList())))
        val partes = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { partes[it.name] = zip.readBytes() }
        }
        assertEquals(
            setOf(
                "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
                "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml",
            ),
            partes.keys,
        )
        // Si algún XML estuviera mal formado, el parser lanza una excepción.
        val parser = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
        partes.values.forEach { parser.parse(ByteArrayInputStream(it)) }

        val hoja = String(partes.getValue("xl/worksheets/sheet1.xml"), Charsets.UTF_8)
        assertTrue("el texto es texto, no fórmula", hoja.contains("=HIPERVINCULO(&quot;malo&quot;)"))
        assertTrue("sin elemento de fórmula", !hoja.contains("<f>"))
        // 21/09/2026 son 46286 días desde el 30/12/1899
        assertTrue(hoja.contains("<c r=\"B2\" s=\"2\"><v>46286</v></c>"))
    }

    @Test
    fun `letras de columna y nombres de hoja validos`() {
        assertEquals("A", EscritorExcel.columna(0))
        assertEquals("Z", EscritorExcel.columna(25))
        assertEquals("AA", EscritorExcel.columna(26))
        assertEquals("AZ", EscritorExcel.columna(51))
        assertEquals("Movimientos 2026 a 2027", EscritorExcel.nombreDeHoja("Movimientos 2026/2027".replace("/", " a ")))
        assertEquals(31, EscritorExcel.nombreDeHoja("x".repeat(40)).length)
        assertEquals("a b", EscritorExcel.nombreDeHoja("a:b"))
    }
}
