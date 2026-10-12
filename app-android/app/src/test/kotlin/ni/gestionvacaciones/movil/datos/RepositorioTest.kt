package ni.gestionvacaciones.movil.datos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ni.gestionvacaciones.movil.dominio.AccionBitacora
import ni.gestionvacaciones.movil.dominio.BorradorAjuste
import ni.gestionvacaciones.movil.dominio.BorradorFuncionario
import ni.gestionvacaciones.movil.dominio.BorradorSolicitud
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.EstadoSolicitud
import ni.gestionvacaciones.movil.dominio.OperacionAjuste
import ni.gestionvacaciones.movil.dominio.ReglaDeNegocioException
import ni.gestionvacaciones.movil.dominio.TipoMovimiento
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pruebas del repositorio contra una base SQLite REAL (en memoria), con
 * Robolectric. Es la parte donde un error hace daño de verdad: el saldo.
 */
@RunWith(RobolectricTestRunner::class)
class RepositorioTest {

    private lateinit var db: BaseDeDatos
    private lateinit var repo: Repositorio
    private var ahora = Instant.parse("2026-09-14T15:00:00Z") // 9:00 am en Managua

    @Before
    fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BaseDeDatos::class.java)
            .allowMainThreadQueries()
            .build()
        repo = Repositorio(db) { ahora.also { ahora = ahora.plusSeconds(1) } }
        runBlocking { repo.guardarNombreUsuaria("Brenda Vásquez") }
    }

    @After
    fun cerrar() = db.close()

    private fun crearMaria(dias: Int = 10) = runBlocking {
        repo.crearFuncionario(BorradorFuncionario("María González", "Asistente", "Contabilidad", null, saldoDias = dias))
    }

    private fun saldoDe(id: Long) = runBlocking { db.funcionarios().obtener(id)!!.saldoMinutos }

    private fun dia(d: Int, m: Int = 9) = LocalDate.of(2026, m, d)

    /** La garantía central: el saldo es SIEMPRE la suma de sus movimientos. */
    private fun verificarLibro(id: Long) = runBlocking {
        val suma = db.movimientos().deFuncionario(id).sumOf { it.minutos }
        assertEquals("el saldo tiene que ser la suma de sus movimientos", suma, saldoDe(id))
    }

    @Test
    fun `el ejemplo de Brenda de punta a punta`() = runBlocking<Unit> {
        val id = crearMaria(10)
        assertEquals("10 días", ConversorTiempo.formatear(saldoDe(id), 8))

        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.VACACIONES, dia(21), dia(21), dias = 1))
        assertEquals("9 días", ConversorTiempo.formatear(saldoDe(id), 8))

        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.CITA_MEDICA, dia(22), dia(22), horas = 2))
        assertEquals("8 días y 6 horas", ConversorTiempo.formatear(saldoDe(id), 8))

        val movimientos = db.movimientos().deFuncionario(id).map { it.tipo }
        assertEquals(listOf(TipoMovimiento.CONSUMO, TipoMovimiento.CONSUMO, TipoMovimiento.SALDO_INICIAL), movimientos)
        verificarLibro(id)
    }

    @Test
    fun `un funcionario con saldo cero no deja movimiento inicial`() = runBlocking<Unit> {
        val id = crearMaria(0)
        assertTrue(db.movimientos().deFuncionario(id).isEmpty())
        verificarLibro(id)
    }

    @Test
    fun `sin autorizacion no se registra mas de lo que hay, y no queda nada a medias`() = runBlocking<Unit> {
        val id = crearMaria(3)
        val borrador = BorradorSolicitud(id, TipoSolicitud.VACACIONES, dia(10, 11), dia(14, 11), dias = 5)

        val error = assertThrows(ReglaDeNegocioException::class.java) { runBlocking { repo.registrarSolicitud(borrador) } }
        assertEquals("Brenda, María González solo tiene 3 días de saldo y estás registrando 5 días.", error.message)
        assertEquals(3 * 480, saldoDe(id))
        assertTrue("no quedó ninguna solicitud guardada", db.solicitudes().todas().isEmpty())

        val cambio = repo.registrarSolicitud(borrador, autorizarNegativo = true, motivoAutorizacion = "Autorizado por la dirección")
        assertEquals("−2 días", ConversorTiempo.formatear(cambio.saldoDespues, 8))
        assertTrue(db.bitacora().todo().any { it.accion == AccionBitacora.SALDO_NEGATIVO_AUTORIZADO })
        verificarLibro(id)
    }

    @Test
    fun `anular devuelve el tiempo, exige motivo y no se puede hacer dos veces`() = runBlocking<Unit> {
        val id = crearMaria(10)
        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.PERMISO, dia(15), dia(15), horas = 3))
        val solicitud = db.solicitudes().todas().single()

        assertThrows(ReglaDeNegocioException::class.java) { runBlocking { repo.anularSolicitud(solicitud.id, "  ") } }

        val cambio = repo.anularSolicitud(solicitud.id, "Se registró por error")
        assertEquals(10 * 480, cambio.saldoDespues)
        val anulada = db.solicitudes().obtener(solicitud.id)!!
        assertEquals(EstadoSolicitud.ANULADA, anulada.estado)
        assertEquals("Se registró por error", anulada.motivoAnulacion)

        val otraVez = assertThrows(ReglaDeNegocioException::class.java) {
            runBlocking { repo.anularSolicitud(solicitud.id, "otra vez") }
        }
        assertTrue(otraVez.message!!.contains("ya estaba anulada"))
        assertEquals("el saldo no se devolvió dos veces", 10 * 480, saldoDe(id))
        verificarLibro(id)
    }

    @Test
    fun `ajuste manual suma y resta con motivo`() = runBlocking<Unit> {
        val id = crearMaria(2)
        repo.ajustarSaldo(id, BorradorAjuste(OperacionAjuste.SUMAR, dias = 1, motivo = "Día compensatorio"))
        assertEquals(3 * 480, saldoDe(id))
        repo.ajustarSaldo(id, BorradorAjuste(OperacionAjuste.RESTAR, horas = 4, motivo = "Corrección"))
        assertEquals("2 días y 4 horas", ConversorTiempo.formatear(saldoDe(id), 8))
        verificarLibro(id)
    }

    @Test
    fun `de baja no recibe solicitudes y al reactivar conserva saldo e historial`() = runBlocking<Unit> {
        val id = crearMaria(5)
        repo.darDeBaja(id)
        assertThrows(ReglaDeNegocioException::class.java) {
            runBlocking { repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.VACACIONES, dia(20), dia(20), dias = 1)) }
        }
        assertThrows(ReglaDeNegocioException::class.java) { runBlocking { repo.darDeBaja(id) } }
        repo.reactivar(id)
        assertEquals(5 * 480, saldoDe(id))
        assertTrue(db.funcionarios().obtener(id)!!.activo)
    }

    @Test
    fun `editar no toca el saldo`() = runBlocking<Unit> {
        val id = crearMaria(4)
        repo.editarFuncionario(id, BorradorFuncionario("María José González", "Jefa", null, null, saldoDias = 99))
        val f = db.funcionarios().obtener(id)!!
        assertEquals("María José González", f.nombreCompleto)
        assertEquals(4 * 480, f.saldoMinutos)
    }

    @Test
    fun `el buscador ignora tildes y oculta a los de baja`() = runBlocking<Unit> {
        repo.crearFuncionario(BorradorFuncionario("Ángela Pérez", null, null, null))
        val id = crearMaria()
        repo.darDeBaja(id)
        val todos = repo.funcionarios.first()
        assertEquals(listOf("Ángela Pérez", "María González"), todos.map { it.nombreCompleto })
        assertEquals(listOf("Ángela Pérez"), repo.buscar(todos, "angela", incluirDadosDeBaja = false).map { it.nombreCompleto })
        assertTrue(repo.buscar(todos, "maria", incluirDadosDeBaja = false).isEmpty())
        assertEquals(1, repo.buscar(todos, "maria", incluirDadosDeBaja = true).size)
    }

    @Test
    fun `el resumen del mes cuenta las registradas, no las anuladas`() = runBlocking<Unit> {
        val id = crearMaria(20)
        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.VACACIONES, dia(1), dia(2), dias = 2))
        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.CITA_MEDICA, dia(3), dia(3), horas = 2))
        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.CITA_MEDICA, dia(4), dia(4), horas = 1))
        repo.anularSolicitud(db.solicitudes().todas().maxBy { it.id }.id, "error")

        val resumen = repo.resumenDelMes(YearMonth.of(2026, 9)).first()
        assertEquals(ResumenTipo(1, 960), resumen[TipoSolicitud.VACACIONES])
        assertEquals(ResumenTipo(1, 120), resumen[TipoSolicitud.CITA_MEDICA])
        assertEquals(ResumenTipo(0, 0), resumen[TipoSolicitud.PERMISO])
    }

    @Test
    fun `cambiar la jornada no toca los minutos guardados`() = runBlocking<Unit> {
        val id = crearMaria(10)
        assertTrue(repo.actualizarAjustes(6, "America/Managua", "Brenda Vásquez"))
        assertEquals(4800, saldoDe(id))
        assertEquals("13 días y 2 horas", ConversorTiempo.formatear(saldoDe(id), repo.parametrosActuales().horasPorJornada))
        assertFalse("sin cambios no se anota nada", repo.actualizarAjustes(6, "America/Managua", "Brenda Vásquez"))
        assertThrows(ReglaDeNegocioException::class.java) {
            runBlocking { repo.actualizarAjustes(8, "Marte/Olympus", "Brenda") }
        }
    }

    @Test
    fun `un respaldo se restaura exacto en un celular nuevo`() = runBlocking<Unit> {
        val id = crearMaria(10)
        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.VACACIONES, dia(21), dia(21), dias = 1, motivo = "Viaje"))
        repo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.CITA_MEDICA, dia(22), dia(22), horas = 2))
        repo.anularSolicitud(db.solicitudes().todas().maxBy { it.id }.id, "Se reprogramó")
        val original = repo.contenidoCompleto()
        val json = Respaldo.escribir(original, ahora)

        // Otro celular: base vacía.
        val nueva = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BaseDeDatos::class.java)
            .allowMainThreadQueries().build()
        try {
            val repoNuevo = Repositorio(nueva) { ahora }
            repoNuevo.restaurar(Respaldo.leer(json), "respaldo.json")
            val restaurado = repoNuevo.contenidoCompleto()
            assertEquals(original.funcionarios, restaurado.funcionarios)
            assertEquals(original.solicitudes, restaurado.solicitudes)
            assertEquals(original.movimientos, restaurado.movimientos)
            assertEquals(original.parametros.toSet(), restaurado.parametros.toSet())
            assertEquals("9 días", ConversorTiempo.formatear(restaurado.funcionarios.single().saldoMinutos, 8))
            // Lo nuevo que se anote no choca con los números de lo restaurado.
            repoNuevo.registrarSolicitud(BorradorSolicitud(id, TipoSolicitud.PERMISO, dia(25), dia(25), horas = 1))
        } finally {
            nueva.close()
        }
    }

    @Test
    fun `un respaldo con el saldo alterado se rechaza y no toca nada`() = runBlocking<Unit> {
        val id = crearMaria(10)
        val json = Respaldo.escribir(repo.contenidoCompleto(), ahora)
        val alterado = json.replace("\"saldo_minutos\": 4800", "\"saldo_minutos\": 99999")
        assertTrue("la prueba sí alteró el archivo", alterado != json)

        val error = assertThrows(ReglaDeNegocioException::class.java) { Respaldo.leer(alterado) }
        assertTrue(error.message!!.contains("no coincide con su historial"))
        assertThrows(ReglaDeNegocioException::class.java) { Respaldo.leer("{\"esto\": \"no es un respaldo\"}") }
        assertThrows(ReglaDeNegocioException::class.java) { Respaldo.leer("basura") }
        assertEquals(4800, saldoDe(id))
    }
}
