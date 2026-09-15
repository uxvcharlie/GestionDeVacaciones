package ni.gestionvacaciones.reporte;

import ni.gestionvacaciones.solicitud.Solicitud;

/** Una solicitud con los datos de su funcionario, para el reporte del mes. */
public record SolicitudConFuncionario(Solicitud solicitud, String nombre, String cargo, String area) {
}
