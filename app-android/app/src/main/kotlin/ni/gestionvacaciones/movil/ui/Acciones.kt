package ni.gestionvacaciones.movil.ui

import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import ni.gestionvacaciones.movil.Contenedor
import ni.gestionvacaciones.movil.dominio.ReglaDeNegocioException

/** El contenedor de la app, disponible en cualquier pantalla. */
val LocalContenedor = staticCompositionLocalOf<Contenedor> { error("Falta el contenedor de la app.") }

/** Muestra un mensaje abajo de la pantalla que sobrevive al cambiar de pantalla. */
val LocalAvisar = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Lo que devuelve una acción: o salió bien, o un mensaje para mostrar. */
sealed interface Resultado<out T> {
    data class Bien<T>(val valor: T) : Resultado<T>
    data class Mal(val mensaje: String, val requiereAutorizacion: Boolean = false) : Resultado<Nothing>
}

const val MENSAJE_ERROR_INESPERADO =
    "Algo salió mal y no se guardó nada. Volvé a intentarlo; si vuelve a pasar, avisale a quien te instaló la app."

/**
 * Ejecuta una acción y convierte cualquier error en un mensaje en español
 * humano. Nunca llega a la pantalla un error técnico: el detalle se escribe en
 * el registro del sistema (Logcat), que es donde sirve para depurar.
 */
suspend fun <T> intentar(accion: suspend () -> T): Resultado<T> = try {
    Resultado.Bien(accion())
} catch (e: ReglaDeNegocioException) {
    Resultado.Mal(e.message ?: MENSAJE_ERROR_INESPERADO, e.requiereAutorizacion)
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    Log.e("Vacaciones", "Error inesperado", e)
    Resultado.Mal(MENSAJE_ERROR_INESPERADO)
}
