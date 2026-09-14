package ni.gestionvacaciones.solicitud;

import java.util.Map;

/**
 * Cuánto tiempo se fue en el año en cada tipo de solicitud. Cuenta solo las
 * solicitudes registradas (no las anuladas), según su fecha de inicio.
 */
public record DesgloseAnual(int anio, Map<TipoSolicitud, Integer> minutosPorTipo) {

    public int minutos(TipoSolicitud tipo) {
        return minutosPorTipo.getOrDefault(tipo, 0);
    }

    public boolean estaVacio() {
        return minutosPorTipo.values().stream().allMatch(minutos -> minutos == 0);
    }
}
