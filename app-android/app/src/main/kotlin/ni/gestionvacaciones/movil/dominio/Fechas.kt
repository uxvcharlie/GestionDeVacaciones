package ni.gestionvacaciones.movil.dominio

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Formatos de fecha y hora, siempre iguales en cualquier celular.
 *
 * Los nombres de días y meses están escritos a mano, en español, a propósito:
 * si dependieran del idioma del teléfono, en un celular configurado en inglés
 * la app mostraría "Monday" y "September".
 */
object Fechas {

    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    // DayOfWeek.value: lunes = 1 ... domingo = 7
    private val DIAS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

    /** 21/09/2026 */
    fun fecha(fecha: LocalDate?): String {
        if (fecha == null) return ""
        // Locale.ROOT: los dígitos siempre son 0-9, sin importar el idioma del celular.
        return "%02d/%02d/%04d".format(Locale.ROOT, fecha.dayOfMonth, fecha.monthValue, fecha.year)
    }

    /** 21/09/2026 3:45 pm, en la zona indicada. Hora de 12 horas con am/pm. */
    fun fechaHora(instante: Instant?, zona: ZoneId): String {
        if (instante == null) return ""
        val local = instante.atZone(zona)
        return fecha(local.toLocalDate()) + " " + hora(local.toLocalTime())
    }

    /** 3:45 pm · 12:05 pm · 12:30 am */
    fun hora(hora: LocalTime): String {
        val h12 = if (hora.hour % 12 == 0) 12 else hora.hour % 12
        val sufijo = if (hora.hour < 12) "am" else "pm"
        return "%d:%02d %s".format(Locale.ROOT, h12, hora.minute, sufijo)
    }

    /** "lunes 14 de septiembre de 2026" */
    fun fechaLarga(fecha: LocalDate): String =
        "${DIAS[fecha.dayOfWeek.value - 1]} ${fecha.dayOfMonth} de ${MESES[fecha.monthValue - 1]} de ${fecha.year}"

    /** "septiembre de 2026" */
    fun mes(mes: YearMonth): String = "${MESES[mes.monthValue - 1]} de ${mes.year}"

    /** "el 21/09/2026" o "del 10/11/2026 al 14/11/2026" */
    fun rango(inicio: LocalDate, fin: LocalDate): String =
        if (inicio == fin) "el ${fecha(inicio)}"
        else "del ${fecha(inicio)} al ${fecha(fin)}"

    /**
     * Cuántos días de calendario abarca el rango, inicio y fin incluidos.
     * Cuenta sábados y domingos: es solo una referencia para Brenda, que es
     * quien escribe la cantidad real.
     */
    fun diasCorridos(inicio: LocalDate, fin: LocalDate): Long =
        if (fin.isBefore(inicio)) 0 else ChronoUnit.DAYS.between(inicio, fin) + 1
}
