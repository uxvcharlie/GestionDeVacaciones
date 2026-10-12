package ni.gestionvacaciones.movil.acceso

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ¿Está abierta la app o hay que pedir el PIN?
 *
 * Igual que la sesión de la web: se cierra a los 30 minutos sin actividad y
 * avisa un minuto antes. Además, cada vez que la app arranca desde cero pide
 * el PIN.
 *
 * Se mide con el reloj interno del teléfono (tiempo desde que se encendió),
 * no con la hora del celular: así cambiar la hora no alarga la sesión.
 */
class ControlSesion(private val reloj: () -> Long = SystemClock::elapsedRealtime) {

    private val _desbloqueada = MutableStateFlow(false)
    val desbloqueada: StateFlow<Boolean> = _desbloqueada.asStateFlow()

    @Volatile
    private var ultimaActividad: Long = reloj()

    fun desbloquear() {
        ultimaActividad = reloj()
        _desbloqueada.value = true
    }

    fun bloquear() {
        _desbloqueada.value = false
    }

    /** Se llama con cada toque en la pantalla. */
    fun registrarActividad() {
        ultimaActividad = reloj()
    }

    fun milisegundosInactiva(): Long = reloj() - ultimaActividad

    /** ¿Ya hay que mostrar el aviso de "se va a bloquear en un minuto"? */
    fun debeAvisar(): Boolean = _desbloqueada.value && milisegundosInactiva() >= INACTIVIDAD_MAXIMA - AVISO_ANTES

    /** Revisa si se pasó el tiempo y, si es así, bloquea. */
    fun revisarInactividad() {
        if (_desbloqueada.value && milisegundosInactiva() >= INACTIVIDAD_MAXIMA) bloquear()
    }

    companion object {
        const val INACTIVIDAD_MAXIMA = 30L * 60 * 1000
        const val AVISO_ANTES = 60L * 1000
    }
}
