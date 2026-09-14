package ni.gestionvacaciones.auditoria;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe en la bitácora.
 *
 * <p>Se une a la transacción de quien llama: si la acción falla y se deshace,
 * su renglón de bitácora también se deshace. Así la bitácora nunca registra
 * algo que en realidad no pasó.</p>
 */
@Service
public class ServicioAuditoria {

    public static final String ENTIDAD_SOLICITUD = "solicitud";
    public static final String ENTIDAD_EMPLEADO = "empleado";

    private final AuditoriaRepositorio repositorio;

    public ServicioAuditoria(AuditoriaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void registrar(Long usuarioId, AccionAuditoria accion, String entidad, Long entidadId,
                          String detalle, String ip) {
        repositorio.save(new Auditoria(usuarioId, accion.name(), entidad, entidadId,
                recortar(detalle, 1000), recortar(ip, 45)));
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo);
    }
}
