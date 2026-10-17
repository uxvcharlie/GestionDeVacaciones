package ni.gestionvacaciones.movil.dominio

/**
 * El corazón del sistema: convierte entre minutos y "días y horas".
 *
 * Es una copia fiel, en Kotlin, de `ConversorTiempo.java` de la web. Las
 * reglas son exactamente las mismas.
 *
 * ## Por qué minutos
 * En la base de datos todo el tiempo es un número entero de minutos. Los días
 * no se guardan nunca: son solo una forma de mostrar esos minutos. Con enteros,
 * sumar y restar da siempre exacto; con decimales, 8.75 días termina siendo
 * 8.749999999998 y eso en un registro laboral es un reclamo.
 *
 * ## La conversión
 * ```
 *   minutos_por_dia = horas_por_jornada × 60        (jornada de 8 h → 480)
 *   días            = minutos ÷ minutos_por_dia     (división entera)
 *   resto           = minutos mód minutos_por_dia
 *   horas           = resto ÷ 60                    (división entera)
 *   minutos sueltos = resto mód 60
 * ```
 *
 * ## El ejemplo de Brenda, con jornada de 8 horas
 * ```
 *   10 días            = 10 × 480     = 4800 minutos  → "10 días"
 *   − 1 día vacaciones = 4800 − 480   = 4320 minutos  → "9 días"
 *   − cita de 2 horas  = 4320 − 120   = 4200 minutos
 *                        4200 ÷ 480 = 8 días, sobran 360 = 6 horas
 *                                                     → "8 días y 6 horas"
 * ```
 */
object ConversorTiempo {

    const val MINUTOS_POR_HORA = 60

    /** Minutos que tiene una jornada completa. */
    fun minutosPorDia(horasPorJornada: Int): Int {
        require(horasPorJornada in 1..24) { "La jornada debe tener entre 1 y 24 horas." }
        return horasPorJornada * MINUTOS_POR_HORA
    }

    /**
     * Convierte días, horas y minutos a minutos totales.
     *
     * Usa aritmética "exacta": si el resultado no cupiera en un entero, lanza un
     * error en vez de dar un número equivocado en silencio.
     */
    fun aMinutos(dias: Int, horas: Int, minutos: Int, horasPorJornada: Int): Int {
        require(dias >= 0 && horas >= 0 && minutos >= 0) {
            "Los días, las horas y los minutos no pueden ser negativos."
        }
        val deDias = Math.multiplyExact(dias, minutosPorDia(horasPorJornada))
        val deHoras = Math.multiplyExact(horas, MINUTOS_POR_HORA)
        return Math.addExact(deDias, Math.addExact(deHoras, minutos))
    }

    /**
     * Muestra un saldo en lenguaje natural. Nunca en decimales.
     *
     * - 4800 → "10 días"
     * - 4200 → "8 días y 6 horas"
     * - 90 → "1 hora y 30 minutos"
     * - 0 → "0 días"
     *
     * Un saldo negativo (solo posible con autorización expresa) se muestra con
     * el signo menos adelante: "−2 días y 4 horas".
     */
    fun formatear(minutos: Int, horasPorJornada: Int): String {
        val minutosDia = minutosPorDia(horasPorJornada)

        // Long: el valor absoluto de Int.MIN_VALUE no cabe en un Int.
        val total = Math.abs(minutos.toLong())
        val dias = total / minutosDia
        val resto = total % minutosDia
        val horas = resto / MINUTOS_POR_HORA
        val sueltos = resto % MINUTOS_POR_HORA

        val partes = buildList {
            if (dias > 0) add(dias.toString() + if (dias == 1L) " día" else " días")
            if (horas > 0) add(horas.toString() + if (horas == 1L) " hora" else " horas")
            if (sueltos > 0) add(sueltos.toString() + if (sueltos == 1L) " minuto" else " minutos")
        }
        if (partes.isEmpty()) return "0 días"
        return (if (minutos < 0) "−" else "") + unir(partes)
    }

    /**
     * Muestra un tiempo solo en horas y minutos, sin pasarlo a días.
     *
     * Así se muestran las citas médicas y los permisos: una cita de 8 horas se
     * lee "8 horas", no "1 día".
     *
     * - 120 → "2 horas"
     * - 90 → "1 hora y 30 minutos"
     * - 45 → "45 minutos"
     */
    fun formatearHoras(minutos: Int): String {
        val total = Math.abs(minutos.toLong())
        val horas = total / MINUTOS_POR_HORA
        val sueltos = total % MINUTOS_POR_HORA

        val partes = buildList {
            if (horas > 0) add(horas.toString() + if (horas == 1L) " hora" else " horas")
            if (sueltos > 0) add(sueltos.toString() + if (sueltos == 1L) " minuto" else " minutos")
        }
        if (partes.isEmpty()) return "0 horas"
        return (if (minutos < 0) "−" else "") + unir(partes)
    }

    /** Igual que [formatear], pero con "+" adelante si suma: "+10 días", "−1 día". */
    fun formatearConSigno(minutos: Int, horasPorJornada: Int): String {
        val texto = formatear(minutos, horasPorJornada)
        return if (minutos > 0) "+$texto" else texto
    }

    /** "a" · "a y b" · "a, b y c" */
    private fun unir(partes: List<String>): String =
        if (partes.size == 1) partes[0]
        else partes.dropLast(1).joinToString(", ") + " y " + partes.last()
}
