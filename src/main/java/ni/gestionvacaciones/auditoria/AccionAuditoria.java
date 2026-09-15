package ni.gestionvacaciones.auditoria;

/**
 * Las acciones que quedan en la bitácora. Se guarda el nombre (por ejemplo
 * SOLICITUD_ANULADA). La pantalla de bitácora llega en la Fase 5, junto con el
 * registro de ingresos al sistema.
 */
public enum AccionAuditoria {

    INGRESO_EXITOSO("Ingresó al sistema"),
    INGRESO_FALLIDO("Intento de ingreso fallido"),
    USUARIO_BLOQUEADO_POR_INTENTOS("Usuario bloqueado por intentos fallidos"),
    USUARIO_CREADO("Creó un usuario"),
    USUARIO_BLOQUEADO("Bloqueó un usuario"),
    USUARIO_DESBLOQUEADO("Desbloqueó un usuario"),
    PASSWORD_RESTABLECIDA("Restableció una contraseña"),
    PASSWORD_CAMBIADA("Cambió su contraseña"),
    PARAMETRO_CAMBIADO("Cambió un parámetro"),
    REPORTE_DESCARGADO("Descargó un reporte"),
    FUNCIONARIO_REGISTRADO("Registró a un funcionario"),
    FUNCIONARIO_DADO_DE_BAJA("Dio de baja a un funcionario"),
    FUNCIONARIO_REACTIVADO("Reactivó a un funcionario"),
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
