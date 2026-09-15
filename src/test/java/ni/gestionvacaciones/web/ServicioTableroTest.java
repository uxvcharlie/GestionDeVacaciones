package ni.gestionvacaciones.web;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.empleado.Empleado;
import ni.gestionvacaciones.empleado.FormularioEmpleado;
import ni.gestionvacaciones.empleado.ServicioEmpleados;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import ni.gestionvacaciones.solicitud.FormularioSolicitud;
import ni.gestionvacaciones.solicitud.ServicioSolicitudes;
import ni.gestionvacaciones.solicitud.Solicitud;
import ni.gestionvacaciones.solicitud.SolicitudReciente;
import ni.gestionvacaciones.solicitud.TipoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El resumen del tablero contra un PostgreSQL real.
 *
 * <p>La base se comparte con las demás pruebas, así que se comparan
 * diferencias (antes y después) y nunca totales absolutos.</p>
 */
class ServicioTableroTest extends PruebaConPostgres {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Managua"));

    @Autowired
    private ServicioTablero servicioTablero;

    @Autowired
    private ServicioEmpleados servicioEmpleados;

    @Autowired
    private ServicioSolicitudes servicioSolicitudes;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Test
    @DisplayName("Saluda por el primer nombre, con el saludo que corresponde a alguna franja")
    void saludaPorElNombre() {
        assertThat(resumen().saludo()).matches("(Buenos días|Buenas tardes|Buenas noches), Brenda");
        assertThat(resumen().hoy()).contains(String.valueOf(HOY.getYear()));
    }

    @Test
    @DisplayName("Cuenta solo los funcionarios activos")
    void cuentaFuncionariosActivos() {
        long antes = resumen().funcionariosActivos();

        Empleado funcionario = crearFuncionario("Tablero Conteo Activos", 1);
        assertThat(resumen().funcionariosActivos()).isEqualTo(antes + 1);
        assertThat(resumen().hayFuncionarios()).isTrue();

        servicioEmpleados.darDeBaja(funcionario.getId(), brenda().getId(), null);
        assertThat(resumen().funcionariosActivos()).isEqualTo(antes);
    }

    @Test
    @DisplayName("Solicitudes del mes por tipo: cuenta las que empiezan este mes y no las anuladas")
    void resumenDelMesPorTipo() {
        Empleado funcionario = crearFuncionario("Tablero Resumen Del Mes", 20);
        ResumenTablero.ResumenTipo antes = resumen().delMes(TipoSolicitud.PERMISO);

        Solicitud deEsteMes = servicioSolicitudes.registrar(
                permiso(funcionario, HOY, 3), brenda(), null).solicitud();
        servicioSolicitudes.registrar(
                permiso(funcionario, HOY.plusMonths(2).withDayOfMonth(1), 1), brenda(), null);

        ResumenTablero.ResumenTipo despues = resumen().delMes(TipoSolicitud.PERMISO);
        assertThat(despues.cantidad()).isEqualTo(antes.cantidad() + 1);
        assertThat(despues.minutos()).isEqualTo(antes.minutos() + 180);

        servicioSolicitudes.anular(deEsteMes.getId(), "Prueba del tablero", brenda(), null);
        assertThat(resumen().delMes(TipoSolicitud.PERMISO)).isEqualTo(antes);
    }

    @Test
    @DisplayName("Últimas solicitudes: la más reciente primero, con el nombre del funcionario")
    void ultimasSolicitudes() {
        Empleado funcionario = crearFuncionario("Tablero Última Solicitud", 5);
        Solicitud solicitud = servicioSolicitudes.registrar(
                permiso(funcionario, HOY, 2), brenda(), null).solicitud();

        List<SolicitudReciente> recientes = resumen().recientes();
        assertThat(recientes).hasSizeBetween(1, 5);
        assertThat(recientes.get(0).solicitud().getId()).isEqualTo(solicitud.getId());
        assertThat(recientes.get(0).nombreFuncionario()).isEqualTo("Tablero Última Solicitud");
    }

    private ResumenTablero resumen() {
        return servicioTablero.resumen(brenda());
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

    private static FormularioSolicitud permiso(Empleado funcionario, LocalDate dia, int horas) {
        FormularioSolicitud formulario = new FormularioSolicitud();
        formulario.setEmpleadoId(funcionario.getId());
        formulario.setTipo(TipoSolicitud.PERMISO);
        formulario.setFechaInicio(dia);
        formulario.setFechaFin(dia);
        formulario.setHoras(horas);
        return formulario;
    }
}
