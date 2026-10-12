package ni.gestionvacaciones.movil.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Los mismos casos que `ConversorTiempoTest.java` de la web. */
class ConversorTiempoTest {

    private val jornada = 8

    @Test
    fun `el ejemplo exacto de Brenda - 10 dias, luego 9, luego 8 dias y 6 horas`() {
        var saldo = ConversorTiempo.aMinutos(10, 0, 0, jornada)
        assertEquals(4800, saldo)
        assertEquals("10 días", ConversorTiempo.formatear(saldo, jornada))

        saldo -= ConversorTiempo.aMinutos(1, 0, 0, jornada) // 1 día de vacaciones el 21/09
        assertEquals(4320, saldo)
        assertEquals("9 días", ConversorTiempo.formatear(saldo, jornada))

        saldo -= ConversorTiempo.aMinutos(0, 2, 0, jornada) // cita médica de 2 horas
        assertEquals(4200, saldo)
        assertEquals("8 días y 6 horas", ConversorTiempo.formatear(saldo, jornada))
    }

    @Test
    fun `formato en lenguaje natural, sin decimales`() {
        val casos = mapOf(
            0 to "0 días",
            480 to "1 día",
            960 to "2 días",
            60 to "1 hora",
            240 to "4 horas",
            1 to "1 minuto",
            90 to "1 hora y 30 minutos",
            540 to "1 día y 1 hora",
            1025 to "2 días, 1 hora y 5 minutos",
            -1500 to "−3 días y 1 hora",
        )
        casos.forEach { (minutos, esperado) ->
            assertEquals("$minutos minutos", esperado, ConversorTiempo.formatear(minutos, jornada))
        }
    }

    @Test
    fun `citas y permisos se muestran en horas y minutos, nunca en dias`() {
        val casos = mapOf(
            0 to "0 horas",
            45 to "45 minutos",
            60 to "1 hora",
            90 to "1 hora y 30 minutos",
            120 to "2 horas",
            480 to "8 horas",
            600 to "10 horas",
        )
        casos.forEach { (minutos, esperado) ->
            assertEquals("$minutos minutos", esperado, ConversorTiempo.formatearHoras(minutos))
        }
    }

    @Test
    fun `medio dia con jornada de 8 horas son 4 horas`() {
        assertEquals(240, ConversorTiempo.aMinutos(0, 4, 0, jornada))
        assertEquals("4 horas", ConversorTiempo.formatear(240, jornada))
    }

    @Test
    fun `cambiar la jornada cambia como se MUESTRA, nunca los minutos guardados`() {
        val guardado = 4800
        assertEquals("10 días", ConversorTiempo.formatear(guardado, 8))
        assertEquals("13 días y 2 horas", ConversorTiempo.formatear(guardado, 6))
    }

    @Test
    fun `el signo mas solo aparece cuando suma`() {
        assertEquals("+10 días", ConversorTiempo.formatearConSigno(4800, jornada))
        assertEquals("−1 día", ConversorTiempo.formatearConSigno(-480, jornada))
    }

    @Test
    fun `no se aceptan cantidades negativas ni jornadas imposibles`() {
        assertThrows(IllegalArgumentException::class.java) { ConversorTiempo.aMinutos(-1, 0, 0, jornada) }
        assertThrows(IllegalArgumentException::class.java) { ConversorTiempo.formatear(100, 0) }
        assertThrows(IllegalArgumentException::class.java) { ConversorTiempo.formatear(100, 25) }
    }

    @Test
    fun `un numero que no cabe en un entero da error, no un saldo equivocado`() {
        assertThrows(ArithmeticException::class.java) { ConversorTiempo.aMinutos(Int.MAX_VALUE, 0, 0, jornada) }
    }
}
