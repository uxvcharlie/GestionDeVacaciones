package ni.gestionvacaciones.web;

import ni.gestionvacaciones.solicitud.SolicitudReciente;
import ni.gestionvacaciones.solicitud.TipoSolicitud;

import java.util.List;
import java.util.Map;

/**
 * Todo lo que muestra el tablero, calculado de una vez.
 *
 * @param saludo              "Buenos días, Brenda"
 * @param hoy                 "lunes 14 de septiembre de 2026"
 * @param mes                 "septiembre de 2026"
 * @param hayFuncionarios     false la primera vez: se muestra el estado vacío
 * @param delMes              cantidad y tiempo de las solicitudes del mes, por tipo
 */
public record ResumenTablero(
        String saludo,
        String hoy,
        String mes,
        long funcionariosActivos,
        boolean hayFuncionarios,
        Map<TipoSolicitud, ResumenTipo> delMes,
        List<SolicitudReciente> recientes) {

    public ResumenTipo delMes(TipoSolicitud tipo) {
        return delMes.getOrDefault(tipo, ResumenTipo.VACIO);
    }

    /** Cuántas solicitudes y cuánto tiempo, de un tipo. */
    public record ResumenTipo(long cantidad, int minutos) {
        public static final ResumenTipo VACIO = new ResumenTipo(0, 0);
    }
}
