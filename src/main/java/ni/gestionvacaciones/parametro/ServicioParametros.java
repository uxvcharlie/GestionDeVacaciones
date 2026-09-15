package ni.gestionvacaciones.parametro;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Lee y cambia los parámetros del sistema, siempre convertidos y validados.
 *
 * <p>Nada de valores quemados en el código: si mañana la jornada pasa de 8 a 6
 * horas, se cambia desde la pantalla de parámetros y el sistema entero lo toma
 * de inmediato. Cada cambio queda en la bitácora con el valor anterior.</p>
 */
@Service
public class ServicioParametros {

    public static final String HORAS_POR_JORNADA = "horas_por_jornada";
    public static final String ZONA_HORARIA = "zona_horaria";

    private final ParametroRepositorio repositorio;
    private final ServicioAuditoria auditoria;

    public ServicioParametros(ParametroRepositorio repositorio, ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    /** Horas de una jornada laboral completa. Siempre entre 1 y 24. */
    @Transactional(readOnly = true)
    public int horasPorJornada() {
        String valor = valor(HORAS_POR_JORNADA);
        try {
            int horas = Integer.parseInt(valor.trim());
            if (horas < 1 || horas > 24) {
                throw new IllegalStateException(
                        "El parámetro horas_por_jornada debe estar entre 1 y 24, y vale " + horas + ".");
            }
            return horas;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "El parámetro horas_por_jornada no es un número entero: \"" + valor + "\".", e);
        }
    }

    /** Zona horaria con la que se muestran fechas y horas. */
    @Transactional(readOnly = true)
    public ZoneId zonaHoraria() {
        String valor = valor(ZONA_HORARIA);
        try {
            return ZoneId.of(valor.trim());
        } catch (DateTimeException e) {
            throw new IllegalStateException(
                    "El parámetro zona_horaria no es una zona válida: \"" + valor + "\".", e);
        }
    }

    /**
     * Guarda los parámetros. Solo escribe (y deja en la bitácora) los que
     * realmente cambiaron.
     *
     * @return true si cambió al menos uno
     */
    @Transactional
    public boolean actualizar(int horasPorJornada, String zonaHoraria, Long usuarioId, String ip) {
        if (horasPorJornada < 1 || horasPorJornada > 24) {
            throw new ReglaDeNegocioException("Las horas por jornada van de 1 a 24.");
        }
        String zona = zonaHoraria == null ? "" : zonaHoraria.trim();
        try {
            ZoneId.of(zona);
        } catch (DateTimeException e) {
            throw new ReglaDeNegocioException(
                    "La zona horaria «" + zona + "» no existe. Para Nicaragua se escribe America/Managua.");
        }

        boolean cambioJornada = cambiar(HORAS_POR_JORNADA, String.valueOf(horasPorJornada), usuarioId, ip);
        boolean cambioZona = cambiar(ZONA_HORARIA, zona, usuarioId, ip);
        return cambioJornada || cambioZona;
    }

    private boolean cambiar(String clave, String valorNuevo, Long usuarioId, String ip) {
        Parametro parametro = repositorio.findById(clave)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro " + clave + " en la base de datos."));
        String anterior = parametro.getValor();
        if (anterior.equals(valorNuevo)) {
            return false;
        }
        parametro.setValor(valorNuevo);
        parametro.setActualizadoEn(Instant.now());
        parametro.setActualizadoPor(usuarioId);
        auditoria.registrar(usuarioId, AccionAuditoria.PARAMETRO_CAMBIADO, ServicioAuditoria.ENTIDAD_PARAMETRO, null,
                clave + ": " + anterior + " → " + valorNuevo, ip);
        return true;
    }

    private String valor(String clave) {
        return repositorio.findById(clave)
                .map(Parametro::getValor)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el parámetro " + clave + " en la base de datos."));
    }
}
