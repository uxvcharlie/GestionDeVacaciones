package ni.gestionvacaciones.movil.dominio

import java.time.LocalTime

/**
 * El saludo del tablero según la hora de Managua (no la del celular: se usa
 * la zona horaria configurada en los ajustes).
 *
 * - 5:00 a 11:59 → "Buenos días"
 * - 12:00 a 18:59 → "Buenas tardes"
 * - 19:00 a 4:59 → "Buenas noches"
 */
object Saludo {
    fun para(hora: LocalTime): String = when (hora.hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}
