package ni.gestionvacaciones.movil.dominio

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Las reglas del PIN de 4 dígitos.
 *
 * Igual que en la web: 5 intentos equivocados bloquean la app 15 minutos. Y
 * como en la web, se rechazan solo los PIN obvios, sin reglas rebuscadas.
 */
object PoliticaPin {

    const val LARGO = 4
    const val INTENTOS_MAXIMOS = 5
    val DURACION_BLOQUEO: Duration = Duration.ofMinutes(15)

    /** Texto que se le muestra a Brenda al crear el PIN, sin tecnicismos. */
    const val EXPLICACION = "4 números. No uses cuatro iguales (1111) ni seguidos (1234 o 4321)."

    /** @return null si el PIN sirve; si no, el mensaje para la pantalla. */
    fun revisar(pin: String): String? {
        if (pin.length != LARGO || !pin.all { it in '0'..'9' }) return "El PIN tiene que tener exactamente 4 números."
        if (pin.toSet().size == 1) return "No uses cuatro números iguales: es lo primero que alguien probaría."
        val pasos = pin.zipWithNext { a, b -> b - a }.toSet()
        if (pasos == setOf(1) || pasos == setOf(-1)) {
            return "No uses números seguidos como 1234 o 4321: es lo primero que alguien probaría."
        }
        return null
    }
}

/**
 * Cuántas veces se equivocó y hasta cuándo está bloqueada la app.
 * Es inmutable: cada fallo devuelve un estado nuevo.
 */
data class EstadoAcceso(val intentosFallidos: Int = 0, val bloqueadoHasta: Instant? = null) {

    fun estaBloqueado(ahora: Instant): Boolean = bloqueadoHasta?.isAfter(ahora) == true

    /** Al quinto fallo se bloquea 15 minutos y el contador vuelve a cero. */
    fun conFallo(ahora: Instant): EstadoAcceso {
        val intentos = intentosFallidos + 1
        return if (intentos >= PoliticaPin.INTENTOS_MAXIMOS) {
            EstadoAcceso(0, ahora.plus(PoliticaPin.DURACION_BLOQUEO))
        } else {
            EstadoAcceso(intentos, null)
        }
    }

    /** Minutos que faltan para poder volver a intentar, redondeado hacia arriba. */
    fun minutosRestantes(ahora: Instant): Long {
        val hasta = bloqueadoHasta ?: return 0
        val segundos = Duration.between(ahora, hasta).seconds
        return if (segundos <= 0) 0 else (segundos + 59) / 60
    }

    val intentosQueQuedan: Int get() = PoliticaPin.INTENTOS_MAXIMOS - intentosFallidos
}

/**
 * Guarda el PIN como un "hash" PBKDF2 con sal aleatoria: el PIN en sí no se
 * guarda en ningún lado.
 *
 * Honestidad técnica: con solo 10.000 combinaciones posibles, ningún hash
 * protege un PIN de 4 dígitos contra alguien que ya copió los archivos de la
 * app. La protección real es otra: los archivos de la app son privados (otras
 * apps no pueden leerlos) y el bloqueo de 15 minutos tras 5 intentos impide
 * probar combinaciones a mano. El hash evita, igual, que el PIN quede escrito
 * en claro.
 */
object HashPin {

    private const val ALGORITMO = "PBKDF2WithHmacSHA256"
    private const val ITERACIONES = 50_000
    private const val BITS = 256

    /** "pbkdf2$50000$<sal>$<hash>" */
    fun calcular(pin: String, aleatorio: SecureRandom = SecureRandom()): String {
        val sal = ByteArray(16).also { aleatorio.nextBytes(it) }
        val hash = derivar(pin, sal, ITERACIONES)
        val b64 = Base64.getEncoder()
        return "pbkdf2\$$ITERACIONES\$${b64.encodeToString(sal)}\$${b64.encodeToString(hash)}"
    }

    /** Compara en tiempo constante, para no dar pistas por cuánto tarda. */
    fun coincide(pin: String, guardado: String): Boolean {
        val partes = guardado.split('$')
        if (partes.size != 4 || partes[0] != "pbkdf2") return false
        val iteraciones = partes[1].toIntOrNull() ?: return false
        val b64 = Base64.getDecoder()
        val sal = runCatching { b64.decode(partes[2]) }.getOrNull() ?: return false
        val esperado = runCatching { b64.decode(partes[3]) }.getOrNull() ?: return false
        return MessageDigest.isEqual(derivar(pin, sal, iteraciones), esperado)
    }

    private fun derivar(pin: String, sal: ByteArray, iteraciones: Int): ByteArray {
        val especificacion = PBEKeySpec(pin.toCharArray(), sal, iteraciones, BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
        }
    }
}
