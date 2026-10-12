package ni.gestionvacaciones.movil.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AccesoTest {

    @Test
    fun `PIN valido y PIN obvios`() {
        assertNull(PoliticaPin.revisar("2718"))
        assertNull(PoliticaPin.revisar("1357"))
        assertNotNull(PoliticaPin.revisar("123"))
        assertNotNull(PoliticaPin.revisar("12a4"))
        assertNotNull(PoliticaPin.revisar("0000"))
        assertNotNull(PoliticaPin.revisar("1234"))
        assertNotNull(PoliticaPin.revisar("6789"))
        assertNotNull(PoliticaPin.revisar("4321"))
    }

    @Test
    fun `al quinto intento fallido se bloquea 15 minutos`() {
        val ahora = Instant.parse("2026-09-14T15:00:00Z")
        var estado = EstadoAcceso()
        repeat(4) {
            estado = estado.conFallo(ahora)
            assertFalse(estado.estaBloqueado(ahora))
        }
        assertEquals(1, estado.intentosQueQuedan)

        estado = estado.conFallo(ahora)
        assertTrue(estado.estaBloqueado(ahora))
        assertEquals(15, estado.minutosRestantes(ahora))
        assertTrue(estado.estaBloqueado(ahora.plus(Duration.ofMinutes(14))))
        assertFalse(estado.estaBloqueado(ahora.plus(Duration.ofMinutes(15))))
    }

    @Test
    fun `los minutos restantes se redondean hacia arriba`() {
        val ahora = Instant.parse("2026-09-14T15:00:00Z")
        val estado = EstadoAcceso(0, ahora.plusSeconds(61))
        assertEquals(2, estado.minutosRestantes(ahora))
        assertEquals(0, estado.minutosRestantes(ahora.plusSeconds(61)))
    }

    @Test
    fun `el PIN nunca se guarda en claro y cada hash es distinto`() {
        val a = HashPin.calcular("2718")
        val b = HashPin.calcular("2718")
        assertFalse(a.contains("2718"))
        assertNotEquals("la sal aleatoria hace que dos hashes del mismo PIN difieran", a, b)
        assertTrue(HashPin.coincide("2718", a))
        assertTrue(HashPin.coincide("2718", b))
        assertFalse(HashPin.coincide("2719", a))
    }

    @Test
    fun `un hash dañado no deja entrar ni revienta`() {
        assertFalse(HashPin.coincide("2718", "basura"))
        assertFalse(HashPin.coincide("2718", "pbkdf2\$x\$y\$z"))
    }
}
