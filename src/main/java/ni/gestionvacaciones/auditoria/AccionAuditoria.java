package ni.gestionvacaciones.auditoria;

/**
 * Las acciones que quedan en la bitácora. Se guarda el nombre (por ejemplo
 * SOLICITUD_ANULADA). La pantalla de bitácora llega en la Fase 5, junto con el
 * registro de ingresos al sistema.
 */
public enum AccionAuditoria {

    SOLICITUD_REGISTRADA("Registró una solicitud"),
    SOLICITUD_ANULADA("Anuló una solicitud"),
    SALDO_AJUSTADO("Ajustó un saldo a mano"),
    SALDO_NEGATIVO_AUTORIZADO("Autorizó dejar un saldo en negativo");

    private final String etiqueta;

    AccionAuditoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
