package ni.gestionvacaciones.admin;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.Auditoria;

import java.util.List;
import java.util.Map;

/** Una página de la bitácora, con los nombres de quienes hicieron cada cosa. */
public record PaginaBitacora(
        List<Auditoria> renglones,
        Map<Long, String> nombres,
        int pagina,
        boolean hayAnterior,
        boolean haySiguiente,
        long total) {

    /** "Registró una solicitud" en vez de SOLICITUD_REGISTRADA. */
    public String etiqueta(String accion) {
        try {
            return AccionAuditoria.valueOf(accion).getEtiqueta();
        } catch (IllegalArgumentException e) {
            return accion;
        }
    }
}
