package ni.gestionvacaciones.solicitud;

/**
 * Solo dos estados. Esto es un registro, no un trámite: no existen "pendiente",
 * "aprobada" ni "rechazada", porque el permiso ya fue autorizado fuera del
 * sistema y Brenda únicamente lo anota.
 */
public enum EstadoSolicitud {

    REGISTRADA("Registrada"),
    ANULADA("Anulada");

    private final String etiqueta;

    EstadoSolicitud(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
