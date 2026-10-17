package ni.gestionvacaciones.movil.dominio

import java.text.Normalizer
import java.util.Locale

/** Utilidades de texto que comparten varias pantallas. */
object Texto {

    /** Quita espacios sobrantes. Un texto vacío se guarda como null, no como "". */
    fun limpiar(texto: String?): String? {
        if (texto == null) return null
        val limpio = texto.trim().replace(Regex("\\s+"), " ")
        return limpio.ifEmpty { null }
    }

    /**
     * "  Ángela   PÉREZ " → "angela perez".
     *
     * Sirve para que el buscador encuentre a "Ángela Pérez" aunque se escriba
     * "angela perez": no importan las tildes ni las mayúsculas.
     */
    fun normalizar(texto: String?): String {
        if (texto == null) return ""
        val sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        return sinTildes.lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
    }

    /** "Brenda Vásquez" → "Brenda". Para el saludo y los mensajes. */
    fun primerNombre(nombreCompleto: String?): String {
        val limpio = limpiar(nombreCompleto) ?: return ""
        return limpio.substringBefore(' ')
    }

    /** "1 funcionario", "3 funcionarios": nunca "1 funcionarios". */
    fun cantidad(n: Int, singular: String, plural: String): String = if (n == 1) "1 $singular" else "$n $plural"

    /**
     * Concordancia del verbo con una cantidad ya escrita:
     * "Se devuelve 1 día", pero "Se devuelven 2 horas" y "Se devuelven 1 día y 4 horas".
     */
    fun verbo(cantidad: String, singular: String, plural: String): String =
        if (Regex("^1 (día|hora|minuto)$").matches(cantidad)) singular else plural

    /** "Ángela Pérez Núñez" → "angela-perez-nunez", para nombres de archivo. */
    fun nombreDeArchivo(nombre: String): String {
        val limpio = normalizar(nombre).replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return limpio.ifEmpty { "sin-nombre" }
    }
}
