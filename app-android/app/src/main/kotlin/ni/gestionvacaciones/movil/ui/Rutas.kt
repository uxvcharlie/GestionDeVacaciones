package ni.gestionvacaciones.movil.ui

import androidx.navigation.NavController

/**
 * Todas las pantallas de la app y cómo se llega a cada una. Las pantallas no
 * conocen al NavController: reciben este objeto y llaman, por ejemplo,
 * `rutas.ficha(id)`. Así una pantalla nunca escribe una dirección a mano.
 */
class Rutas(private val nav: NavController) {

    companion object {
        const val TABLERO = "tablero"
        const val FUNCIONARIOS = "funcionarios"
        const val NUEVO_FUNCIONARIO = "funcionario/nuevo"
        const val FICHA = "funcionario/{id}"
        const val EDITAR = "funcionario/{id}/editar"
        const val AJUSTE = "funcionario/{id}/ajuste"
        const val SOLICITUD = "solicitud?funcionario={funcionario}"
        const val REPORTES = "reportes"
        const val RESPALDO = "respaldo"
        const val BITACORA = "bitacora"
        const val AJUSTES = "ajustes"
        const val CAMBIAR_PIN = "ajustes/pin"
    }

    fun volver() {
        nav.popBackStack()
    }

    fun tablero() = nav.navigate(TABLERO) { popUpTo(TABLERO) { inclusive = true } }
    fun funcionarios() = nav.navigate(FUNCIONARIOS)
    fun nuevoFuncionario() = nav.navigate(NUEVO_FUNCIONARIO)

    fun ficha(id: Long) = nav.navigate("funcionario/$id")

    /** Después de crear: la ficha reemplaza al formulario, así "volver" no regresa al formulario vacío. */
    fun fichaReemplazando(id: Long) = nav.navigate("funcionario/$id") {
        popUpTo(NUEVO_FUNCIONARIO) { inclusive = true }
    }

    /** Después de registrar sin venir de la ficha: se abre la ficha en lugar del formulario. */
    fun fichaTrasSolicitud(id: Long) = nav.navigate("funcionario/$id") {
        popUpTo(SOLICITUD) { inclusive = true }
    }

    fun editar(id: Long) = nav.navigate("funcionario/$id/editar")
    fun ajuste(id: Long) = nav.navigate("funcionario/$id/ajuste")
    fun solicitud(funcionarioId: Long? = null) = nav.navigate("solicitud?funcionario=${funcionarioId ?: -1}")
    fun reportes() = nav.navigate(REPORTES)
    fun respaldo() = nav.navigate(RESPALDO)
    fun bitacora() = nav.navigate(BITACORA)
    fun ajustes() = nav.navigate(AJUSTES)
    fun cambiarPin() = nav.navigate(CAMBIAR_PIN)
}
