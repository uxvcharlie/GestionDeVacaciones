package ni.gestionvacaciones.movil.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EvaluadorSolicitudTest {

    private val jornada = 8
    private val hoy = LocalDate.of(2026, 9, 14)
    private val maria = FuncionarioEvaluado("María González", activo = true, saldoMinutos = 10 * 480)

    private fun vacaciones(inicio: LocalDate, fin: LocalDate, dias: Int, horas: Int = 0) =
        BorradorSolicitud(1, TipoSolicitud.VACACIONES, inicio, fin, dias = dias, horasSueltas = horas)

    private fun evaluar(
        borrador: BorradorSolicitud,
        funcionario: FuncionarioEvaluado = maria,
        traslapes: List<SolicitudTraslapada> = emptyList(),
    ) = EvaluadorSolicitud.evaluar(borrador, funcionario, traslapes, hoy, jornada, "Brenda Vásquez")

    @Test
    fun `1 dia de vacaciones el 21-09 deja 9 dias, inmediatamente`() {
        val dia = LocalDate.of(2026, 9, 21)
        val e = evaluar(vacaciones(dia, dia, 1))
        assertFalse(e.tieneErrores)
        assertFalse(e.requiereAutorizacion)
        assertEquals(480, e.minutos)
        assertEquals("9 días", ConversorTiempo.formatear(e.saldoDespues, jornada))
    }

    @Test
    fun `una cita de 2 horas descuenta proporcional, no un dia completo`() {
        val conNueve = maria.copy(saldoMinutos = 9 * 480)
        val dia = LocalDate.of(2026, 9, 22)
        val cita = BorradorSolicitud(1, TipoSolicitud.CITA_MEDICA, dia, dia, horas = 2)
        val e = evaluar(cita, conNueve)
        assertEquals(120, e.minutos)
        assertEquals("8 días y 6 horas", ConversorTiempo.formatear(e.saldoDespues, jornada))
    }

    @Test
    fun `los tres tipos descuentan del mismo saldo exactamente igual`() {
        val dia = LocalDate.of(2026, 10, 1)
        val resultados = TipoSolicitud.entries.map { tipo ->
            val b = if (tipo.seCuentaEnDias) BorradorSolicitud(1, tipo, dia, dia, horasSueltas = 3)
            else BorradorSolicitud(1, tipo, dia, dia, horas = 3)
            evaluar(b).saldoDespues
        }
        assertEquals(1, resultados.toSet().size)
        assertEquals(4800 - 180, resultados.first())
    }

    @Test
    fun `si no alcanza el saldo, el mensaje es claro y con nombres`() {
        val conTres = maria.copy(saldoMinutos = 3 * 480)
        val e = evaluar(vacaciones(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 14), 5), conTres)
        assertTrue(e.requiereAutorizacion)
        assertEquals(
            "Brenda, María González solo tiene 3 días de saldo y estás registrando 5 días.",
            e.mensajeSaldoInsuficiente,
        )
    }

    @Test
    fun `sin autorizacion no se puede guardar en negativo`() {
        val conTres = maria.copy(saldoMinutos = 3 * 480)
        val e = evaluar(vacaciones(hoy, hoy.plusDays(4), 5), conTres)

        val sinMarcar = assertThrows(ReglaDeNegocioException::class.java) {
            EvaluadorSolicitud.autorizarGuardado(e, autorizarNegativo = false, motivoAutorizacion = null)
        }
        assertTrue(sinMarcar.requiereAutorizacion)

        val sinMotivo = assertThrows(ReglaDeNegocioException::class.java) {
            EvaluadorSolicitud.autorizarGuardado(e, autorizarNegativo = true, motivoAutorizacion = "   ")
        }
        assertTrue(sinMotivo.message!!.contains("motivo"))

        assertTrue(EvaluadorSolicitud.autorizarGuardado(e, true, "Autorizado por la dirección"))
    }

    @Test
    fun `si el saldo alcanza no hace falta autorizacion`() {
        val e = evaluar(vacaciones(hoy, hoy, 1))
        assertFalse(EvaluadorSolicitud.autorizarGuardado(e, false, null))
    }

    @Test
    fun `fechas al reves bloquean`() {
        val e = evaluar(vacaciones(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 18), 1))
        assertTrue(e.tieneErrores)
        assertEquals("La fecha final no puede ser anterior a la fecha de inicio.", e.errores.first())
        assertThrows(ReglaDeNegocioException::class.java) {
            EvaluadorSolicitud.autorizarGuardado(e, true, "x")
        }
    }

    @Test
    fun `cantidad cero bloquea`() {
        val e = evaluar(vacaciones(hoy, hoy, 0))
        assertTrue(e.tieneErrores)
        assertTrue(e.errores.first().contains("No puede ser cero"))
    }

    @Test
    fun `un funcionario de baja no recibe solicitudes`() {
        val e = evaluar(vacaciones(hoy, hoy, 1), maria.copy(activo = false))
        assertTrue(e.tieneErrores)
        assertTrue(e.errores.first().contains("está de baja"))
    }

    @Test
    fun `valores fuera de rango bloquean aunque la pantalla los haya dejado pasar`() {
        val cita = BorradorSolicitud(1, TipoSolicitud.PERMISO, hoy, hoy, horas = 2, minutos = 75)
        assertEquals("Los minutos van de 0 a 59.", evaluar(cita).errores.first())
    }

    @Test
    fun `fechas a mas de un ano solo advierten`() {
        val lejos = hoy.plusYears(1).plusDays(1)
        val e = evaluar(vacaciones(lejos, lejos, 1))
        assertFalse(e.tieneErrores)
        assertEquals(1, e.advertencias.size)

        val viejo = hoy.minusYears(1).minusDays(1)
        assertEquals(1, evaluar(vacaciones(viejo, viejo, 1)).advertencias.size)
    }

    @Test
    fun `un traslape advierte pero no bloquea`() {
        val otra = SolicitudTraslapada(TipoSolicitud.PERMISO, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12))
        val e = evaluar(vacaciones(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 14), 5), traslapes = listOf(otra))
        assertFalse(e.tieneErrores)
        assertEquals(
            "Ya hay una solicitud de permiso el 12/11/2026 que se cruza con estas fechas. Revisá que no sea la misma.",
            e.advertencias.single(),
        )
    }

    @Test
    fun `los dias corridos cuentan sabados y domingos`() {
        // Del lunes 9 al domingo 15 de noviembre de 2026: 7 días corridos.
        val e = evaluar(vacaciones(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15), 5))
        assertEquals(7, e.diasCorridos)
        assertNull(e.mensajeSaldoInsuficiente)
    }

    @Test
    fun `la descripcion distingue un dia de un rango`() {
        val d = LocalDate.of(2026, 9, 21)
        assertEquals("Cita médica el 21/09/2026", EvaluadorSolicitud.describir(TipoSolicitud.CITA_MEDICA, d, d))
        assertEquals(
            "Vacaciones del 10/11/2026 al 14/11/2026",
            EvaluadorSolicitud.describir(TipoSolicitud.VACACIONES, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 14)),
        )
    }
}
