package ni.gestionvacaciones.solicitud;

import ni.gestionvacaciones.empleado.Empleado;

import java.time.LocalDate;
import java.util.List;

/**
 * Todo lo que el sistema sabe de una solicitud ANTES de guardarla: el
 * resumen, cómo quedaría el saldo, qué la bloquea y qué conviene revisar.
 *
 * @param errores                   bloquean siempre (fechas al revés, cantidad cero, funcionario de baja)
 * @param advertencias              no bloquean (fechas lejanas, traslape con otra solicitud)
 * @param mensajeSaldoInsuficiente  null si el saldo alcanza; si no, el mensaje para Brenda.
 *                                  Solo se puede guardar con autorización expresa.
 */
public record EvaluacionSolicitud(
        Empleado empleado,
        TipoSolicitud tipo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int minutos,
        int saldoAntes,
        int saldoDespues,
        long diasCorridos,
        List<String> errores,
        List<String> advertencias,
        String mensajeSaldoInsuficiente) {

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    public boolean requiereAutorizacion() {
        return mensajeSaldoInsuficiente != null;
    }

    public boolean deUnSoloDia() {
        return fechaInicio.equals(fechaFin);
    }
}
