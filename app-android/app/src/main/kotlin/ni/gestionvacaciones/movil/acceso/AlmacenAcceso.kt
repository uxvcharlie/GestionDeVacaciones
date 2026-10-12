package ni.gestionvacaciones.movil.acceso

import android.content.Context
import androidx.core.content.edit
import ni.gestionvacaciones.movil.dominio.EstadoAcceso
import ni.gestionvacaciones.movil.dominio.HashPin
import ni.gestionvacaciones.movil.dominio.PoliticaPin
import java.time.Instant

/** Qué pasó al escribir el PIN. */
sealed interface ResultadoPin {
    data object Correcto : ResultadoPin
    data class Incorrecto(val intentosQueQuedan: Int) : ResultadoPin
    /** Este intento fue el quinto: la app acaba de bloquearse. */
    data class RecienBloqueado(val minutos: Long) : ResultadoPin
    data class Bloqueado(val minutos: Long) : ResultadoPin
}

/**
 * Guarda el PIN (como hash, nunca en claro) y el contador de intentos.
 *
 * Está en un archivo de preferencias privado de la app, aparte de la base de
 * datos: así el PIN no viaja dentro de los respaldos.
 */
class AlmacenAcceso(contexto: Context, private val reloj: () -> Instant = Instant::now) {

    private val prefs = contexto.getSharedPreferences("acceso", Context.MODE_PRIVATE)

    fun hayPin(): Boolean = prefs.contains(CLAVE_HASH)

    /** @return null si se guardó; si no, el mensaje de por qué el PIN no sirve. */
    fun crearPin(pin: String): String? {
        PoliticaPin.revisar(pin)?.let { return it }
        prefs.edit {
            putString(CLAVE_HASH, HashPin.calcular(pin))
            putInt(CLAVE_INTENTOS, 0)
            remove(CLAVE_BLOQUEADO_HASTA)
        }
        return null
    }

    fun estado(): EstadoAcceso = EstadoAcceso(
        intentosFallidos = prefs.getInt(CLAVE_INTENTOS, 0),
        bloqueadoHasta = prefs.getLong(CLAVE_BLOQUEADO_HASTA, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli),
    )

    fun verificar(pin: String): ResultadoPin {
        val ahora = reloj()
        val actual = estado()
        if (actual.estaBloqueado(ahora)) return ResultadoPin.Bloqueado(actual.minutosRestantes(ahora))

        val guardado = prefs.getString(CLAVE_HASH, null)
        if (guardado != null && HashPin.coincide(pin, guardado)) {
            prefs.edit {
                putInt(CLAVE_INTENTOS, 0)
                remove(CLAVE_BLOQUEADO_HASTA)
            }
            return ResultadoPin.Correcto
        }

        val nuevo = actual.conFallo(ahora)
        prefs.edit {
            putInt(CLAVE_INTENTOS, nuevo.intentosFallidos)
            if (nuevo.bloqueadoHasta != null) putLong(CLAVE_BLOQUEADO_HASTA, nuevo.bloqueadoHasta.toEpochMilli())
            else remove(CLAVE_BLOQUEADO_HASTA)
        }
        return if (nuevo.estaBloqueado(ahora)) ResultadoPin.RecienBloqueado(nuevo.minutosRestantes(ahora))
        else ResultadoPin.Incorrecto(nuevo.intentosQueQuedan)
    }

    private companion object {
        const val CLAVE_HASH = "pin_hash"
        const val CLAVE_INTENTOS = "intentos_fallidos"
        const val CLAVE_BLOQUEADO_HASTA = "bloqueado_hasta"
    }
}
