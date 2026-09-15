package ni.gestionvacaciones.empleado;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ConversorTiempo;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.parametro.ServicioParametros;
import ni.gestionvacaciones.saldo.ServicioSaldo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Agregar, buscar, editar, dar de baja y reactivar funcionarios.
 *
 * <p>No existe "borrar": el historial de un trabajador no se puede perder.
 * Registrar, dar de baja y reactivar quedan en la bitácora.</p>
 */
@Service
public class ServicioEmpleados {

    private final EmpleadoRepositorio repositorio;
    private final ServicioSaldo servicioSaldo;
    private final ServicioParametros parametros;
    private final ServicioAuditoria auditoria;

    public ServicioEmpleados(EmpleadoRepositorio repositorio,
                             ServicioSaldo servicioSaldo,
                             ServicioParametros parametros,
                             ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.servicioSaldo = servicioSaldo;
        this.parametros = parametros;
        this.auditoria = auditoria;
    }

    /**
     * Busca por nombre sin importar tildes ni mayúsculas: "angela perez"
     * encuentra a "Ángela Pérez".
     *
     * <p>El filtro se hace en Java y no en SQL a propósito: PostgreSQL necesita
     * la extensión {@code unaccent} para ignorar tildes, y con unas decenas de
     * funcionarios no hace falta sumar esa pieza.</p>
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

    /** ¿Hay al menos un funcionario, activo o no? Sirve para el estado vacío. */
    @Transactional(readOnly = true)
    public boolean hayEmpleados() {
        return repositorio.count() > 0;
    }

    @Transactional(readOnly = true)
    public Empleado obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new EmpleadoNoEncontradoException(id));
    }

    /** Igual que {@link #crear(FormularioEmpleado, Long, String)}, sin dirección IP. */
    @Transactional
    public Empleado crear(FormularioEmpleado formulario, Long realizadoPor) {
        return crear(formulario, realizadoPor, null);
    }

    /**
     * Registra un funcionario con su saldo inicial.
     *
     * <p>Todo en una transacción: si falla el saldo, tampoco queda el funcionario.</p>
     */
    @Transactional
    public Empleado crear(FormularioEmpleado formulario, Long realizadoPor, String ip) {
        int jornada = parametros.horasPorJornada();
        int minutos = ConversorTiempo.aMinutos(formulario.getSaldoDias(), formulario.getSaldoHoras(), 0, jornada);

        Empleado empleado = new Empleado();
        copiarDatos(formulario, empleado);
        empleado = repositorio.save(empleado);

        servicioSaldo.registrarSaldoInicial(empleado, minutos, realizadoPor);

        auditoria.registrar(realizadoPor, AccionAuditoria.FUNCIONARIO_REGISTRADO,
                ServicioAuditoria.ENTIDAD_EMPLEADO, empleado.getId(),
                empleado.getNombreCompleto() + " · Saldo inicial: " + ConversorTiempo.formatear(minutos, jornada), ip);
        return empleado;
    }

    /** Cambia los datos. El saldo NO se toca aunque venga en el formulario. */
    @Transactional
    public Empleado actualizar(Long id, FormularioEmpleado formulario) {
        Empleado empleado = obtener(id);
        copiarDatos(formulario, empleado);
        return empleado;
    }

    /** Baja lógica: el funcionario queda guardado con todo su historial. */
    @Transactional
    public Empleado darDeBaja(Long id, Long realizadoPor, String ip) {
        Empleado empleado = obtener(id);
        if (!empleado.isActivo()) {
            throw new ReglaDeNegocioException(empleado.getNombreCompleto() + " ya estaba de baja.");
        }
        empleado.setActivo(false);
        auditoria.registrar(realizadoPor, AccionAuditoria.FUNCIONARIO_DADO_DE_BAJA,
                ServicioAuditoria.ENTIDAD_EMPLEADO, id, empleado.getNombreCompleto(), ip);
        return empleado;
    }

    /**
     * Deshace una baja: el funcionario vuelve a aparecer al registrar
     * solicitudes. Su saldo y su historial siguen exactamente como estaban.
     */
    @Transactional
    public Empleado reactivar(Long id, Long realizadoPor, String ip) {
        Empleado empleado = obtener(id);
        if (empleado.isActivo()) {
            throw new ReglaDeNegocioException(empleado.getNombreCompleto() + " no está de baja.");
        }
        empleado.setActivo(true);
        auditoria.registrar(realizadoPor, AccionAuditoria.FUNCIONARIO_REACTIVADO,
                ServicioAuditoria.ENTIDAD_EMPLEADO, id, empleado.getNombreCompleto(), ip);
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
