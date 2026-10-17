package ni.gestionvacaciones.movil.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReglasDeSaldoTest {

    private val jornada = 8
    private val maria = FuncionarioEvaluado("María González", activo = true, saldoMinutos = 3 * 480)

    @Test
    fun `restar mas de lo que hay se rechaza sin autorizacion`() {
        val error = assertThrows(ReglaDeNegocioException::class.java) {
            ReglasDeSaldo.aplicar(saldoAntes = 100, minutos = -101, permitirNegativo = false)
        }
        assertTrue(error.requiereAutorizacion)
    }

    @Test
    fun `con autorizacion el saldo puede quedar negativo`() {
        assertEquals(-1, ReglasDeSaldo.aplicar(100, -101, permitirNegativo = true))
    }

    @Test
    fun `sumar nunca necesita autorizacion, aunque se parta de un negativo`() {
        assertEquals(380, ReglasDeSaldo.aplicar(-100, 480, permitirNegativo = false))
    }

    @Test
    fun `un movimiento de cero no existe`() {
        assertThrows(IllegalArgumentException::class.java) { ReglasDeSaldo.aplicar(100, 0, false) }
    }

    @Test
    fun `las descripciones largas se recortan`() {
        val largo = "x".repeat(400)
        assertEquals(300, ReglasDeSaldo.recortar(largo)!!.length)
        assertTrue(ReglasDeSaldo.recortar(largo)!!.endsWith("…"))
        assertEquals("corto", ReglasDeSaldo.recortar("corto"))
    }

    // --- Ajuste manual -------------------------------------------------------

    private fun ajuste(op: OperacionAjuste?, dias: Int = 0, horas: Int = 0, motivo: String? = "Corrección", autorizar: Boolean = false) =
        BorradorAjuste(op, dias, horas, 0, motivo, autorizar)

    @Test
    fun `el ajuste exige motivo`() {
        val e = assertThrows(ReglaDeNegocioException::class.java) {
            ReglasDeAjuste.revisar(ajuste(OperacionAjuste.SUMAR, dias = 1, motivo = "  "), maria, jornada, "Brenda")
        }
        assertEquals("Escribí el motivo del ajuste. Queda guardado en el historial.", e.message)
    }

    @Test
    fun `el ajuste exige elegir sumar o restar y una cantidad`() {
        assertThrows(ReglaDeNegocioException::class.java) {
            ReglasDeAjuste.revisar(ajuste(null, dias = 1), maria, jornada, "Brenda")
        }
        assertThrows(ReglaDeNegocioException::class.java) {
            ReglasDeAjuste.revisar(ajuste(OperacionAjuste.SUMAR), maria, jornada, "Brenda")
        }
    }

    @Test
    fun `sumar devuelve minutos positivos y restar negativos`() {
        assertEquals(480, ReglasDeAjuste.revisar(ajuste(OperacionAjuste.SUMAR, dias = 1), maria, jornada, "Brenda").minutosConSigno)
        assertEquals(-120, ReglasDeAjuste.revisar(ajuste(OperacionAjuste.RESTAR, horas = 2), maria, jornada, "Brenda").minutosConSigno)
    }

    @Test
    fun `restar de mas pide autorizacion con un mensaje claro`() {
        val e = assertThrows(ReglaDeNegocioException::class.java) {
            ReglasDeAjuste.revisar(ajuste(OperacionAjuste.RESTAR, dias = 5), maria, jornada, "Brenda Vásquez")
        }
        assertTrue(e.requiereAutorizacion)
        assertTrue(e.message!!.startsWith("Brenda, María González solo tiene 3 días de saldo y estás restando 5 días."))

        val autorizado = ReglasDeAjuste.revisar(ajuste(OperacionAjuste.RESTAR, dias = 5, autorizar = true), maria, jornada, "Brenda")
        assertTrue(autorizado.quedaNegativo)
    }

    @Test
    fun `a un funcionario de baja no se le ajusta el saldo`() {
        assertThrows(ReglaDeNegocioException::class.java) {
            ReglasDeAjuste.revisar(ajuste(OperacionAjuste.SUMAR, dias = 1), maria.copy(activo = false), jornada, "Brenda")
        }
    }

    @Test
    fun `un ajuste que no deja negativo no lo marca como negativo`() {
        assertFalse(ReglasDeAjuste.revisar(ajuste(OperacionAjuste.RESTAR, dias = 3), maria, jornada, "Brenda").quedaNegativo)
    }

    // --- Funcionarios --------------------------------------------------------

    @Test
    fun `alta de funcionario - nombre obligatorio y fecha de ingreso no futura`() {
        val hoy = LocalDate.of(2026, 9, 14)
        val errores = ReglasDeFuncionario.revisar(
            BorradorFuncionario("   ", null, null, hoy.plusDays(1), saldoDias = 0, saldoHoras = 24), hoy, esAlta = true,
        )
        assertEquals(setOf("nombre", "fechaIngreso", "saldoHoras"), errores.keys)
    }

    @Test
    fun `al editar no se revisa el saldo`() {
        val hoy = LocalDate.of(2026, 9, 14)
        val errores = ReglasDeFuncionario.revisar(
            BorradorFuncionario("María González", null, null, null, saldoDias = -5), hoy, esAlta = false,
        )
        assertTrue(errores.isEmpty())
    }
}
