package ni.gestionvacaciones.saldo;

import ni.gestionvacaciones.empleado.Empleado;

/** Cómo quedó el saldo después de un movimiento: antes y después, en minutos. */
public record CambioDeSaldo(Empleado empleado, int saldoAntes, int saldoDespues) {
}
