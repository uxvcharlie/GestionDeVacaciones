package ni.gestionvacaciones.movil.dominio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class FechasTextoSaludoTest {

    private val managua = ZoneId.of("America/Managua")

    @Test
    fun `fechas en formato dd-MM-yyyy`() {
        assertEquals("21/09/2026", Fechas.fecha(LocalDate.of(2026, 9, 21)))
        assertEquals("01/02/2020", Fechas.fecha(LocalDate.of(2020, 2, 1)))
        assertEquals("", Fechas.fecha(null))
    }

    @Test
    fun `hora de 12 horas con am y pm`() {
        assertEquals("12:30 am", Fechas.hora(LocalTime.of(0, 30)))
        assertEquals("9:05 am", Fechas.hora(LocalTime.of(9, 5)))
        assertEquals("12:00 pm", Fechas.hora(LocalTime.of(12, 0)))
        assertEquals("3:45 pm", Fechas.hora(LocalTime.of(15, 45)))
    }

    @Test
    fun `fecha y hora en la zona de Managua, no en la del celular`() {
        // 21:45 UTC son las 3:45 pm en Managua (UTC−6, sin horario de verano).
        val instante = ZonedDateTime.of(2026, 9, 21, 21, 45, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("21/09/2026 3:45 pm", Fechas.fechaHora(instante, managua))
    }

    @Test
    fun `fecha larga y mes en espanol, sin depender del idioma del celular`() {
        assertEquals("lunes 14 de septiembre de 2026", Fechas.fechaLarga(LocalDate.of(2026, 9, 14)))
        assertEquals("domingo 1 de noviembre de 2026", Fechas.fechaLarga(LocalDate.of(2026, 11, 1)))
        assertEquals("septiembre de 2026", Fechas.mes(YearMonth.of(2026, 9)))
    }

    @Test
    fun `saludo segun la hora`() {
        assertEquals("Buenas noches", Saludo.para(LocalTime.of(4, 59)))
        assertEquals("Buenos días", Saludo.para(LocalTime.of(5, 0)))
        assertEquals("Buenos días", Saludo.para(LocalTime.of(11, 59)))
        assertEquals("Buenas tardes", Saludo.para(LocalTime.of(12, 0)))
        assertEquals("Buenas tardes", Saludo.para(LocalTime.of(18, 59)))
        assertEquals("Buenas noches", Saludo.para(LocalTime.of(19, 0)))
    }

    @Test
    fun `el buscador ignora tildes, mayusculas y espacios`() {
        assertEquals("angela perez", Texto.normalizar("  Ángela   PÉREZ "))
        assertEquals(Texto.normalizar("Núñez"), Texto.normalizar("nunez"))
    }

    @Test
    fun `limpiar convierte el texto vacio en null`() {
        assertEquals(null, Texto.limpiar("   "))
        assertEquals("María González", Texto.limpiar("  María   González "))
    }

    @Test
    fun `primer nombre y nombre de archivo`() {
        assertEquals("Brenda", Texto.primerNombre("Brenda Vásquez"))
        assertEquals("angela-perez-nunez", Texto.nombreDeArchivo("Ángela Pérez Núñez"))
        assertEquals("sin-nombre", Texto.nombreDeArchivo("¿?"))
    }
}
