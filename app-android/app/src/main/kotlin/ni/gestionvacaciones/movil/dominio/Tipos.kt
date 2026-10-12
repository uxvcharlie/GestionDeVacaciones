package ni.gestionvacaciones.movil.dominio

/**
 * Los tres tipos de solicitud.
 *
 * El tipo NO cambia el cálculo: los tres descuentan igual del mismo saldo.
 * Solo cambia cómo se escribe y se muestra la cantidad (las vacaciones en días,
 * las citas y los permisos en horas) y sirve para desglosar los reportes.
 */
enum class TipoSolicitud(val etiqueta: String, val seCuentaEnDias: Boolean, val icono: String) {
    VACACIONES("Vacaciones", true, "🏖"),
    CITA_MEDICA("Cita médica", false, "🏥"),
    PERMISO("Permiso", false, "📝");

    /** "5 días" para vacaciones; "2 horas y 30 minutos" para citas y permisos. */
    fun formatear(minutos: Int, horasPorJornada: Int): String =
        if (seCuentaEnDias) ConversorTiempo.formatear(minutos, horasPorJornada)
        else ConversorTiempo.formatearHoras(minutos)
}

/**
 * Solo dos estados. Esto es un registro, no un trámite: no existen
 * "pendiente", "aprobada" ni "rechazada", porque el permiso ya fue autorizado
 * fuera del sistema y Brenda únicamente lo anota.
 */
enum class EstadoSolicitud(val etiqueta: String) {
    REGISTRADA("Registrada"),
    ANULADA("Anulada"),
}

/** Los cuatro tipos de movimiento del libro contable del saldo. */
enum class TipoMovimiento(val etiqueta: String) {
    /** El saldo con el que se registra al funcionario. */
    SALDO_INICIAL("Saldo inicial"),

    /** Una suma o resta hecha a mano, siempre con motivo. */
    AJUSTE_MANUAL("Ajuste manual"),

    /** El descuento automático al registrar una solicitud. */
    CONSUMO("Descuento por solicitud"),

    /** La devolución automática al anular una solicitud. */
    REVERSION("Devolución por anulación"),
}

/** Las acciones que quedan en la bitácora. */
enum class AccionBitacora(val etiqueta: String) {
    PIN_CREADO("Creó el PIN de acceso"),
    PIN_CAMBIADO("Cambió el PIN de acceso"),
    INGRESO_FALLIDO("Intento de ingreso con PIN equivocado"),
    BLOQUEO_POR_INTENTOS("La app se bloqueó por intentos fallidos"),
    PARAMETRO_CAMBIADO("Cambió un ajuste"),
    REPORTE_GENERADO("Generó un reporte"),
    RESPALDO_CREADO("Creó un respaldo"),
    RESPALDO_RESTAURADO("Restauró un respaldo"),
    FUNCIONARIO_REGISTRADO("Registró a un funcionario"),
    FUNCIONARIO_EDITADO("Editó los datos de un funcionario"),
    FUNCIONARIO_DADO_DE_BAJA("Dio de baja a un funcionario"),
    FUNCIONARIO_REACTIVADO("Reactivó a un funcionario"),
    SOLICITUD_REGISTRADA("Registró una solicitud"),
    SOLICITUD_ANULADA("Anuló una solicitud"),
    SALDO_AJUSTADO("Ajustó un saldo a mano"),
    SALDO_NEGATIVO_AUTORIZADO("Autorizó dejar un saldo en negativo"),
}
