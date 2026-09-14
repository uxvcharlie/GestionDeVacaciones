package ni.gestionvacaciones.empleado;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.saldo.MovimientoSaldo;
import ni.gestionvacaciones.saldo.ServicioSaldo;
import ni.gestionvacaciones.saldo.TipoMovimiento;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reglas de empleados contra una base PostgreSQL real: saldo inicial con su
 * movimiento, baja lógica sin perder historial y buscador sin tildes.
 */
class ServicioEmpleadosTest extends PruebaConPostgres {

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    @Autowired
    private ServicioSaldo servicioSaldo;

    @Autowired
    private EmpleadoRepositorio empleadoRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Test
    @DisplayName("Registrar con 10 días guarda 4800 minutos y deja su movimiento SALDO_INICIAL")
    void saldoInicialDejaSuMovimiento() {
        Empleado empleado = servicioEmpleados.crear(formulario("Prueba Diez Días", 10, 0), idAdmin());

        Empleado guardado = empleadoRepositorio.findById(empleado.getId()).orElseThrow();
        assertThat(guardado.getSaldoVacacionesMinutos()).isEqualTo(4800);

        List<MovimientoSaldo> historial = servicioSaldo.historial(empleado.getId());
        assertThat(historial).hasSize(1);
        MovimientoSaldo movimiento = historial.get(0);
        assertThat(movimiento.getTipoMovimiento()).isEqualTo(TipoMovimiento.SALDO_INICIAL);
        assertThat(movimiento.getMinutos()).isEqualTo(4800);
        assertThat(movimiento.getSaldoResultanteMinutos()).isEqualTo(4800);
        assertThat(movimiento.getRealizadoPor()).isEqualTo(idAdmin());
    }

    @Test
    @DisplayName("Medio día se registra: 2 días y 4 horas son 1200 minutos")
    void saldoInicialConHoras() {
        Empleado empleado = servicioEmpleados.crear(formulario("Prueba Medio Día", 2, 4), idAdmin());

        assertThat(empleadoRepositorio.findById(empleado.getId()).orElseThrow().getSaldoVacacionesMinutos())
                .isEqualTo(1200);
    }

    @Test
    @DisplayName("Con saldo inicial cero no se escribe ningún movimiento")
    void saldoCeroNoDejaMovimiento() {
        Empleado empleado = servicioEmpleados.crear(formulario("Prueba Sin Saldo", 0, 0), idAdmin());

        assertThat(servicioSaldo.historial(empleado.getId())).isEmpty();
        assertThat(empleadoRepositorio.findById(empleado.getId()).orElseThrow().getSaldoVacacionesMinutos())
                .isZero();
    }

    @Test
    @DisplayName("Editar los datos no toca el saldo, aunque el formulario traiga otro número")
    void editarNoTocaElSaldo() {
        Empleado empleado = servicioEmpleados.crear(formulario("Prueba Edición", 3, 0), idAdmin());

        FormularioEmpleado cambios = formulario("Prueba Edición Corregida", 99, 0);
        cambios.setCargo("Asistente");
        servicioEmpleados.actualizar(empleado.getId(), cambios);

        Empleado guardado = empleadoRepositorio.findById(empleado.getId()).orElseThrow();
        assertThat(guardado.getNombreCompleto()).isEqualTo("Prueba Edición Corregida");
        assertThat(guardado.getCargo()).isEqualTo("Asistente");
        assertThat(guardado.getSaldoVacacionesMinutos()).isEqualTo(1440);
        assertThat(servicioSaldo.historial(empleado.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Dar de baja no borra: el empleado y su historial siguen guardados")
    void bajaLogicaConservaHistorial() {
        Empleado empleado = servicioEmpleados.crear(formulario("Prueba Baja Lógica", 5, 0), idAdmin());

        servicioEmpleados.darDeBaja(empleado.getId());

        Empleado guardado = empleadoRepositorio.findById(empleado.getId()).orElseThrow();
        assertThat(guardado.isActivo()).isFalse();
        assertThat(servicioSaldo.historial(empleado.getId())).hasSize(1);

        assertThat(servicioEmpleados.buscar("Prueba Baja Lógica", false)).isEmpty();
        assertThat(servicioEmpleados.buscar("Prueba Baja Lógica", true))
                .extracting(Empleado::getId).containsExactly(empleado.getId());
    }

    @Test
    @DisplayName("El buscador ignora tildes, mayúsculas y espacios de más")
    void buscadorSinTildes() {
        Empleado empleado = servicioEmpleados.crear(formulario("Ángela Pérez Núñez", 1, 0), idAdmin());

        assertThat(servicioEmpleados.buscar("  angela   PEREZ ", false))
                .extracting(Empleado::getId).contains(empleado.getId());
        assertThat(servicioEmpleados.buscar("nunez", false))
                .extracting(Empleado::getId).contains(empleado.getId());
    }

    @Test
    @DisplayName("El saldo no se puede modificar fuera de una transacción")
    void saldoSoloDentroDeUnaTransaccion() {
        Empleado empleado = servicioEmpleados.crear(formulario("Prueba Transacción", 0, 0), idAdmin());

        assertThatThrownBy(() -> servicioSaldo.registrarSaldoInicial(empleado, 60, idAdmin()))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(empleadoRepositorio.findById(empleado.getId()).orElseThrow().getSaldoVacacionesMinutos())
                .isZero();
    }

    private Long idAdmin() {
        return usuarioRepositorio.findByUsername("brenda").orElseThrow().getId();
    }

    private static FormularioEmpleado formulario(String nombre, int dias, int horas) {
        FormularioEmpleado formulario = new FormularioEmpleado();
        formulario.setNombreCompleto(nombre);
        formulario.setSaldoDias(dias);
        formulario.setSaldoHoras(horas);
        return formulario;
    }
}
