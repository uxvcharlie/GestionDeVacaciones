package ni.gestionvacaciones.movil.dominio

import java.time.LocalDate

/**
 * Los datos de una solicitud mientras se completa, antes de guardarla.
 *
 * La cantidad tiene campos distintos según el tipo, porque se piensa distinto:
 * las vacaciones en días (con horas sueltas para el medio día), y las citas y
 * los permisos en horas y minutos. Solo se usan los que corresponden al tipo.
 */
data class BorradorSolicitud(
    val funcionarioId: Long,
    val tipo: TipoSolicitud,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    // Vacaciones
    val dias: Int = 0,
    val horasSueltas: Int = 0,
    // Citas médicas y permisos
    val horas: Int = 0,
    val minutos: Int = 0,
    val motivo: String? = null,
) {
    /** Minutos totales, leyendo solo los campos que corresponden al tipo. */
    fun minutosTotales(horasPorJornada: Int): Int =
        if (tipo.seCuentaEnDias) ConversorTiempo.aMinutos(dias, horasSueltas, 0, horasPorJornada)
        else ConversorTiempo.aMinutos(0, horas, minutos, horasPorJornada)
}

/** Lo que el evaluador necesita saber del funcionario. */
data class FuncionarioEvaluado(
    val nombreCompleto: String,
    val activo: Boolean,
    val saldoMinutos: Int,
)

/** Otra solicitud REGISTRADA del mismo funcionario que se cruza con estas fechas. */
data class SolicitudTraslapada(
    val tipo: TipoSolicitud,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
)

/**
 * Todo lo que se sabe de una solicitud ANTES de guardarla: el resumen, cómo
 * quedaría el saldo, qué la bloquea y qué conviene revisar.
 *
 * @param errores bloquean siempre (fechas al revés, cantidad cero, funcionario de baja)
 * @param advertencias no bloquean (fechas lejanas, traslape con otra solicitud)
 * @param mensajeSaldoInsuficiente null si el saldo alcanza; si no, el mensaje
 *        para Brenda. Solo se puede guardar con autorización expresa.
 */
data class EvaluacionSolicitud(
    val funcionario: FuncionarioEvaluado,
    val tipo: TipoSolicitud,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate,
    val minutos: Int,
    val saldoAntes: Int,
    val saldoDespues: Int,
    val diasCorridos: Long,
    val errores: List<String>,
    val advertencias: List<String>,
    val mensajeSaldoInsuficiente: String?,
) {
    val tieneErrores: Boolean get() = errores.isNotEmpty()
    val requiereAutorizacion: Boolean get() = mensajeSaldoInsuficiente != null
}

/**
 * Las reglas de una solicitud, en un solo lugar y sin tocar la base de datos.
 * Es la misma lógica que `ServicioSolicitudes.evaluar` de la web.
 *
 * ## Las reglas
 * 1. Los tres tipos descuentan del MISMO saldo, igual.
 * 2. El saldo no puede quedar negativo, salvo autorización expresa con motivo.
 * 3. Fechas al revés bloquean. Fechas a más de un año, o que se cruzan con
 *    otra solicitud, solo advierten.
 *
 * Se llama dos veces: al mostrar el resumen y de nuevo al guardar, porque
 * entre una y otra el saldo pudo cambiar.
 */
object EvaluadorSolicitud {

    const val LARGO_MOTIVO = 300
    const val LARGO_MOTIVO_AUTORIZACION = 250

    fun evaluar(
        borrador: BorradorSolicitud,
        funcionario: FuncionarioEvaluado,
        traslapes: List<SolicitudTraslapada>,
        hoy: LocalDate,
        horasPorJornada: Int,
        nombreUsuaria: String,
    ): EvaluacionSolicitud {
        val inicio = borrador.fechaInicio
        val fin = borrador.fechaFin
        val tipo = borrador.tipo

        val errores = mutableListOf<String>()
        val advertencias = mutableListOf<String>()

        if (!funcionario.activo) {
            errores += "${funcionario.nombreCompleto} está de baja: no se le pueden registrar solicitudes."
        }
        val fechasAlReves = fin.isBefore(inicio)
        if (fechasAlReves) {
            errores += "La fecha final no puede ser anterior a la fecha de inicio."
        }

        // Los mismos límites que el formulario de la web. La pantalla ya los
        // impide, pero la regla final está acá: la pantalla nunca es la única
        // barrera.
        val fueraDeRango = if (tipo.seCuentaEnDias) {
            when {
                borrador.dias !in 0..366 -> "Revisá los días: van de 0 a 366 en una sola solicitud."
                borrador.horasSueltas !in 0..23 -> "Las horas sueltas van de 0 a 23."
                else -> null
            }
        } else {
            when {
                borrador.horas !in 0..23 -> "Las horas van de 0 a 23."
                borrador.minutos !in 0..59 -> "Los minutos van de 0 a 59."
                else -> null
            }
        }
        val minutos = if (fueraDeRango != null) {
            errores += fueraDeRango
            0
        } else {
            borrador.minutosTotales(horasPorJornada)
        }
        if (fueraDeRango == null && minutos <= 0) {
            errores += if (tipo.seCuentaEnDias) "Escribí cuántos días u horas son. No puede ser cero."
            else "Escribí cuántas horas o minutos son. No puede ser cero."
        }
        if ((borrador.motivo?.length ?: 0) > LARGO_MOTIVO) {
            errores += "El motivo puede tener hasta $LARGO_MOTIVO caracteres."
        }

        if (inicio.isBefore(hoy.minusYears(1))) {
            advertencias += "La fecha de inicio es de hace más de un año. Revisá que esté bien escrita."
        }
        if (fin.isAfter(hoy.plusYears(1))) {
            advertencias += "La fecha final es dentro de más de un año. Revisá que esté bien escrita."
        }
        if (!fechasAlReves) {
            for (otra in traslapes) {
                advertencias += "Ya hay una solicitud de ${otra.tipo.etiqueta.lowercase()} " +
                    "${Fechas.rango(otra.fechaInicio, otra.fechaFin)} que se cruza con estas fechas. " +
                    "Revisá que no sea la misma."
            }
        }

        val saldoAntes = funcionario.saldoMinutos
        val saldoDespues = saldoAntes - maxOf(minutos, 0)
        val mensajeSaldo = if (errores.isEmpty() && saldoDespues < 0) {
            val quien = Texto.primerNombre(nombreUsuaria).ifEmpty { "Atención" }
            "$quien, ${funcionario.nombreCompleto} solo tiene " +
                "${ConversorTiempo.formatear(saldoAntes, horasPorJornada)} de saldo y estás registrando " +
                "${tipo.formatear(minutos, horasPorJornada)}."
        } else {
            null
        }

        return EvaluacionSolicitud(
            funcionario = funcionario,
            tipo = tipo,
            fechaInicio = inicio,
            fechaFin = fin,
            minutos = minutos,
            saldoAntes = saldoAntes,
            saldoDespues = saldoDespues,
            diasCorridos = Fechas.diasCorridos(inicio, fin),
            errores = errores.toList(),
            advertencias = advertencias.toList(),
            mensajeSaldoInsuficiente = mensajeSaldo,
        )
    }

    /**
     * Decide si una solicitud evaluada se puede guardar.
     *
     * @return true si queda en negativo con autorización (hay que dejarlo en la bitácora)
     * @throws ReglaDeNegocioException si no se puede guardar
     */
    fun autorizarGuardado(
        evaluacion: EvaluacionSolicitud,
        autorizarNegativo: Boolean,
        motivoAutorizacion: String?,
    ): Boolean {
        if (evaluacion.tieneErrores) throw ReglaDeNegocioException(evaluacion.errores.first())
        if (!evaluacion.requiereAutorizacion) return false

        val mensaje = evaluacion.mensajeSaldoInsuficiente!!
        if (!autorizarNegativo) throw ReglaDeNegocioException(mensaje, requiereAutorizacion = true)
        val motivo = Texto.limpiar(motivoAutorizacion)
            ?: throw ReglaDeNegocioException(
                "Para dejar el saldo en negativo tenés que escribir el motivo de la autorización.",
                requiereAutorizacion = true,
            )
        if (motivo.length > LARGO_MOTIVO_AUTORIZACION) {
            throw ReglaDeNegocioException(
                "El motivo de la autorización puede tener hasta $LARGO_MOTIVO_AUTORIZACION caracteres.",
                requiereAutorizacion = true,
            )
        }
        return true
    }

    /** "Vacaciones del 10/11/2026 al 14/11/2026" o "Cita médica el 21/09/2026". */
    fun describir(tipo: TipoSolicitud, inicio: LocalDate, fin: LocalDate): String =
        "${tipo.etiqueta} ${Fechas.rango(inicio, fin)}"
}
