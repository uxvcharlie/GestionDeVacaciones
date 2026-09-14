package ni.gestionvacaciones.empleado;

import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.saldo.ServicioSaldo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Agregar, buscar, editar y dar de baja empleados.
 *
 * <p>No existe "borrar": el historial de un trabajador no se puede perder.</p>
 */
@Service
public class ServicioEmpleados {

    private final EmpleadoRepositorio repositorio;
    private final ServicioSaldo servicioSaldo;
    private final ServicioParametros parametros;

    public ServicioEmpleados(EmpleadoRepositorio repositorio,
                             ServicioSaldo servicioSaldo,
                             ServicioParametros parametros) {
        this.repositorio = repositorio;
        this.servicioSaldo = servicioSaldo;
        this.parametros = parametros;
    }

    /**
     * Busca por nombre sin importar tildes ni mayúsculas: "angela perez"
     * encuentra a "Ángela Pérez".
     *
     * <p>El filtro se hace en Java y no en SQL a propósito: PostgreSQL necesita
     * la extensión {@code unaccent} para ignorar tildes, y con unas decenas de
     * empleados no hace falta sumar esa pieza.</p>
     */
    @Transactional(readOnly = true)
    public List<Empleado> buscar(String texto, boolean incluirDadosDeBaja) {
        List<Empleado> candidatos = incluirDadosDeBaja
                ? repositorio.findAllByOrderByNombreCompletoAsc()
                : repositorio.findByActivoTrueOrderByNombreCompletoAsc();

        String buscado = normalizar(texto);
        if (buscado.isEmpty()) {
            return candidatos;
        }
        return candidatos.stream()
                .filter(empleado -> normalizar(empleado.getNombreCompleto()).contains(buscado))
                .toList();
    }

    /** ¿Hay al menos un empleado, activo o no? Sirve para el estado vacío. */
    @Transactional(readOnly = true)
    public boolean hayEmpleados() {
        return repositorio.count() > 0;
    }

    @Transactional(readOnly = true)
    public Empleado obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new EmpleadoNoEncontradoException(id));
    }

    /**
     * Registra un empleado con su saldo inicial.
     *
     * <p>Todo en una transacción: si falla el saldo, tampoco queda el empleado.</p>
     */
    @Transactional
    public Empleado crear(FormularioEmpleado formulario, Long realizadoPor) {
        int minutos = ConversorTiempo.aMinutos(
                formulario.getSaldoDias(), formulario.getSaldoHoras(), 0, parametros.horasPorJornada());

        Empleado empleado = new Empleado();
        copiarDatos(formulario, empleado);
        empleado = repositorio.save(empleado);

        servicioSaldo.registrarSaldoInicial(empleado, minutos, realizadoPor);
        return empleado;
    }

    /** Cambia los datos. El saldo NO se toca aunque venga en el formulario. */
    @Transactional
    public Empleado actualizar(Long id, FormularioEmpleado formulario) {
        Empleado empleado = obtener(id);
        copiarDatos(formulario, empleado);
        return empleado;
    }

    /** Baja lógica: el empleado queda guardado con todo su historial. */
    @Transactional
    public Empleado darDeBaja(Long id) {
        Empleado empleado = obtener(id);
        empleado.setActivo(false);
        return empleado;
    }

    private static void copiarDatos(FormularioEmpleado formulario, Empleado empleado) {
        empleado.setNombreCompleto(limpiar(formulario.getNombreCompleto()));
        empleado.setCargo(limpiar(formulario.getCargo()));
        empleado.setAreaODependencia(limpiar(formulario.getAreaODependencia()));
        empleado.setFechaIngreso(formulario.getFechaIngreso());
    }

    /** Quita espacios sobrantes. Un texto vacío se guarda como NULL, no como "". */
    static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }

    /** "  Ángela   PÉREZ " → "angela perez" */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
