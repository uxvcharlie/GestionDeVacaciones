package ni.gestionvacaciones.saldo;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.auditoria.Auditoria;
import ni.gestionvacaciones.auditoria.AuditoriaRepositorio;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.EmpleadoRepositorio;
import ni.gestionvacaciones.empleado.FormularioEmpleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ajuste manual de saldo contra un PostgreSQL real. */
class ServicioSaldoTest extends PruebaConPostgres {

    @Autowired
    private ServicioSaldo servicioSaldo;

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    @Autowired
    private EmpleadoRepositorio empleadoRepositorio;

    @Autowired
    private AuditoriaRepositorio auditoriaRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Test
    @DisplayName("Sumar y restar a mano, con motivo: deja AJUSTE_MANUAL y queda en la bitácora")
    void sumarYRestar() {
        Empleado funcionario = crearFuncionario("Ajuste Suma Y Resta", 2);

        servicioSaldo.ajustarManual(funcionario.getId(), ajuste(FormularioAjuste.Operacion.SUMAR, 0, 4, 0, "Corrección de saldo inicial"), brenda(), null);
        assertThat(saldoTexto(funcionario)).isEqualTo("2 días y 4 horas");

        servicioSaldo.ajustarManual(funcionario.getId(), ajuste(FormularioAjuste.Operacion.RESTAR, 1, 0, 30, "Día descontado por planilla"), brenda(), null);
        assertThat(saldo(funcionario)).isEqualTo(1200 - 510);

        MovimientoSaldo ultimo = servicioSaldo.historial(funcionario.getId()).get(0);
        assertThat(ultimo.getTipoMovimiento()).isEqualTo(TipoMovimiento.AJUSTE_MANUAL);
        assertThat(ultimo.getMinutos()).isEqualTo(-510);
        assertThat(ultimo.getDescripcion()).isEqualTo("Día descontado por planilla");

        assertThat(auditoriaRepositorio.findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(
                ServicioAuditoria.ENTIDAD_EMPLEADO, funcionario.getId()))
                .extracting(Auditoria::getAccion).containsOnly("SALDO_AJUSTADO").hasSize(2);
    }

    @Test
    @DisplayName("Sin motivo, o de cero minutos, el ajuste no se guarda")
    void ajusteInvalido() {
        Empleado funcionario = crearFuncionario("Ajuste Inválido", 2);

        assertThatThrownBy(() -> servicioSaldo.ajustarManual(funcionario.getId(),
                ajuste(FormularioAjuste.Operacion.SUMAR, 1, 0, 0, "  "), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("motivo");
        assertThatThrownBy(() -> servicioSaldo.ajustarManual(funcionario.getId(),
                ajuste(FormularioAjuste.Operacion.SUMAR, 0, 0, 0, "Nada"), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("cero");

        assertThat(saldoTexto(funcionario)).isEqualTo("2 días");
        assertThat(servicioSaldo.historial(funcionario.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Una resta que deja el saldo en negativo pide autorización; con ella, queda en la bitácora")
    void restaANegativo() {
        Empleado funcionario = crearFuncionario("Ajuste A Negativo", 1);

        assertThatThrownBy(() -> servicioSaldo.ajustarManual(funcionario.getId(),
                ajuste(FormularioAjuste.Operacion.RESTAR, 2, 0, 0, "Descuento de planilla"), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageStartingWith("Brenda, Ajuste A Negativo solo tiene 1 día de saldo y estás restando 2 días.")
                .satisfies(e -> assertThat(((ReglaDeNegocioException) e).requiereAutorizacion()).isTrue());
        assertThat(saldoTexto(funcionario)).isEqualTo("1 día");

        FormularioAjuste autorizado = ajuste(FormularioAjuste.Operacion.RESTAR, 2, 0, 0, "Descuento de planilla");
        autorizado.setAutorizarSaldoNegativo(true);
        servicioSaldo.ajustarManual(funcionario.getId(), autorizado, brenda(), null);

        assertThat(saldoTexto(funcionario)).isEqualTo("−1 día");
        assertThat(auditoriaRepositorio.findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(
                ServicioAuditoria.ENTIDAD_EMPLEADO, funcionario.getId()))
                .extracting(Auditoria::getAccion).contains("SALDO_AJUSTADO", "SALDO_NEGATIVO_AUTORIZADO");
    }

    private Usuario brenda() {
        return usuarioRepositorio.findByUsername("brenda").orElseThrow();
    }

    private Empleado crearFuncionario(String nombre, int dias) {
        FormularioEmpleado formulario = new FormularioEmpleado();
        formulario.setNombreCompleto(nombre);
        formulario.setSaldoDias(dias);
        formulario.setSaldoHoras(0);
        return servicioEmpleados.crear(formulario, brenda().getId());
    }

    private int saldo(Empleado funcionario) {
        return empleadoRepositorio.findById(funcionario.getId()).orElseThrow().getSaldoVacacionesMinutos();
    }

    private String saldoTexto(Empleado funcionario) {
        return ConversorTiempo.formatear(saldo(funcionario), 8);
    }

    private static FormularioAjuste ajuste(FormularioAjuste.Operacion operacion, int dias, int horas, int minutos, String motivo) {
        FormularioAjuste formulario = new FormularioAjuste();
        formulario.setOperacion(operacion);
        formulario.setDias(dias);
        formulario.setHoras(horas);
        formulario.setMinutos(minutos);
        formulario.setMotivo(motivo);
        return formulario;
    }
}
