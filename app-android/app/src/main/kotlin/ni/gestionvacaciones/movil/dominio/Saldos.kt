package ni.gestionvacaciones.movil.dominio

/**
 * Las reglas del saldo. Es la red de seguridad final: aunque la pantalla ya
 * haya revisado todo, cada movimiento vuelve a pasar por acá.
 */
object ReglasDeSaldo {

    const val LARGO_DESCRIPCION = 300

    /**
     * Cómo queda el saldo después de un movimiento.
     *
     * @param minutos positivo suma, negativo resta; nunca cero
     * @param permitirNegativo true solo con autorización expresa
     * @throws ReglaDeNegocioException si la resta deja el saldo en negativo sin autorización
     */
    fun aplicar(saldoAntes: Int, minutos: Int, permitirNegativo: Boolean): Int {
        require(minutos != 0) { "Un movimiento de saldo no puede ser de cero minutos." }
        val despues = Math.addExact(saldoAntes, minutos)
        if (minutos < 0 && despues < 0 && !permitirNegativo) {
            throw ReglaDeNegocioException("El saldo no alcanza para este movimiento.", requiereAutorizacion = true)
        }
        return despues
    }

    /** Recorta una descripción larga para que quepa en el historial. */
    fun recortar(texto: String?): String? =
        if (texto == null || texto.length <= LARGO_DESCRIPCION) texto
        else texto.take(LARGO_DESCRIPCION - 1) + "…"
}

/** Sumar o restar en un ajuste manual. */
enum class OperacionAjuste { SUMAR, RESTAR }

/** El formulario del ajuste manual de saldo. */
data class BorradorAjuste(
    val operacion: OperacionAjuste?,
    val dias: Int = 0,
    val horas: Int = 0,
    val minutos: Int = 0,
    val motivo: String?,
    val autorizarNegativo: Boolean = false,
)

/** Resultado de revisar un ajuste: cuánto mueve (con signo) y si deja el saldo en negativo. */
data class AjusteRevisado(val minutosConSigno: Int, val motivo: String, val quedaNegativo: Boolean)

/**
 * El ajuste manual: sumar o restar tiempo, siempre con motivo. Misma lógica que
 * `ServicioSaldo.ajustarManual` de la web.
 */
object ReglasDeAjuste {

    fun revisar(
        borrador: BorradorAjuste,
        funcionario: FuncionarioEvaluado,
        horasPorJornada: Int,
        nombreUsuaria: String,
    ): AjusteRevisado {
        val motivo = Texto.limpiar(borrador.motivo)
            ?: throw ReglaDeNegocioException("Escribí el motivo del ajuste. Queda guardado en el historial.")
        if (motivo.length > ReglasDeSaldo.LARGO_DESCRIPCION) {
            throw ReglaDeNegocioException("El motivo puede tener hasta ${ReglasDeSaldo.LARGO_DESCRIPCION} caracteres.")
        }
        val operacion = borrador.operacion
            ?: throw ReglaDeNegocioException("Elegí si vas a sumar o a restar.")
        when {
            borrador.dias !in 0..3650 -> throw ReglaDeNegocioException("Revisá los días: más de 3650 parece un error de tipeo.")
            borrador.horas !in 0..23 -> throw ReglaDeNegocioException("Las horas van de 0 a 23.")
            borrador.minutos !in 0..59 -> throw ReglaDeNegocioException("Los minutos van de 0 a 59.")
        }
        val total = ConversorTiempo.aMinutos(borrador.dias, borrador.horas, borrador.minutos, horasPorJornada)
        if (total == 0) throw ReglaDeNegocioException("Escribí cuánto vas a sumar o restar. No puede ser cero.")
        if (!funcionario.activo) {
            throw ReglaDeNegocioException("${funcionario.nombreCompleto} está de baja: no se le puede ajustar el saldo.")
        }

        val conSigno = if (operacion == OperacionAjuste.RESTAR) -total else total
        val quedaNegativo = conSigno < 0 && funcionario.saldoMinutos.toLong() + conSigno < 0
        if (quedaNegativo && !borrador.autorizarNegativo) {
            val quien = Texto.primerNombre(nombreUsuaria).ifEmpty { "Atención" }
            throw ReglaDeNegocioException(
                "$quien, ${funcionario.nombreCompleto} solo tiene " +
                    "${ConversorTiempo.formatear(funcionario.saldoMinutos, horasPorJornada)} de saldo y estás restando " +
                    "${ConversorTiempo.formatear(total, horasPorJornada)}. Si está autorizado que quede en negativo, " +
                    "marcá la casilla de autorización.",
                requiereAutorizacion = true,
            )
        }
        return AjusteRevisado(conSigno, motivo, quedaNegativo)
    }
}

/** Los datos del alta o edición de un funcionario. */
data class BorradorFuncionario(
    val nombreCompleto: String?,
    val cargo: String?,
    val areaODependencia: String?,
    val fechaIngreso: java.time.LocalDate?,
    // Solo al agregar. Al editar se ignoran: el saldo únicamente cambia con
    // solicitudes o con un ajuste manual.
    val saldoDias: Int = 0,
    val saldoHoras: Int = 0,
)

object ReglasDeFuncionario {

    /** Revisa los datos y devuelve los errores en español, campo por campo. */
    fun revisar(borrador: BorradorFuncionario, hoy: java.time.LocalDate, esAlta: Boolean): Map<String, String> {
        val errores = linkedMapOf<String, String>()
        val nombre = Texto.limpiar(borrador.nombreCompleto)
        if (nombre == null) errores["nombre"] = "Escribí el nombre completo."
        else if (nombre.length > 150) errores["nombre"] = "El nombre puede tener hasta 150 caracteres."
        if ((Texto.limpiar(borrador.cargo)?.length ?: 0) > 100) errores["cargo"] = "El cargo puede tener hasta 100 caracteres."
        if ((Texto.limpiar(borrador.areaODependencia)?.length ?: 0) > 100) {
            errores["area"] = "El área o dependencia puede tener hasta 100 caracteres."
        }
        if (borrador.fechaIngreso?.isAfter(hoy) == true) {
            errores["fechaIngreso"] = "La fecha de ingreso no puede estar en el futuro."
        }
        if (esAlta) {
            if (borrador.saldoDias !in 0..3650) errores["saldoDias"] = "Revisá los días: van de 0 a 3650."
            if (borrador.saldoHoras !in 0..23) {
                errores["saldoHoras"] = "Las horas van de 0 a 23. Si son más, sumalas como días."
            }
        }
        return errores
    }
}
