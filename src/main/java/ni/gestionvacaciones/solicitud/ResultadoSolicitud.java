package ni.gestionvacaciones.solicitud;

import ni.gestionvacaciones.saldo.CambioDeSaldo;

/** La solicitud recién registrada o anulada, y cómo quedó el saldo. */
public record ResultadoSolicitud(Solicitud solicitud, CambioDeSaldo cambio) {
}
