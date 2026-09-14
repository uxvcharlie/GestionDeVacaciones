package ni.gestionvacaciones.saldo;

/**
 * Los cuatro tipos de movimiento del libro contable del saldo.
 * La base de datos rechaza cualquier otro valor con una restricción CHECK.
 */
public enum TipoMovimiento {

    /** El saldo con el que Brenda registra al empleado. */
    SALDO_INICIAL("Saldo inicial"),

    /** Una suma o resta manual hecha por un ADMIN, siempre con motivo. (Fase 3) */
    AJUSTE_MANUAL("Ajuste manual"),

    /** El descuento automático al registrar una solicitud. (Fase 3) */
    CONSUMO("Descuento por solicitud"),

    /** La devolución automática al anular una solicitud. (Fase 3) */
    REVERSION("Devolución por anulación");

    private final String etiqueta;

    TipoMovimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Cómo se muestra en pantalla, en español y sin mayúsculas sostenidas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
