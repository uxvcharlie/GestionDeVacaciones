package ni.gestionvacaciones.admin;

import ni.gestionvacaciones.auditoria.AccionAuditoria;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.seguridad.GeneradorPasswordTemporal;
import ni.gestionvacaciones.seguridad.Rol;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioNoEncontradoException;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * El panel de usuarios: crear, bloquear, desbloquear y restablecer contraseñas.
 *
 * <p>No existe registro público. Solo desde acá se crean usuarios, y cada
 * persona recibe una contraseña temporal que está obligada a cambiar al entrar.
 * Todo queda en la bitácora.</p>
 */
@Service
public class ServicioAdministracionUsuarios {

    /** El usuario y su contraseña temporal, que se muestra una única vez. */
    public record UsuarioConPassword(Usuario usuario, String passwordTemporal) {
    }

    private final UsuarioRepositorio repositorio;
    private final ServicioUsuario servicioUsuario;
    private final PasswordEncoder codificador;
    private final GeneradorPasswordTemporal generador;
    private final ServicioAuditoria auditoria;

    public ServicioAdministracionUsuarios(UsuarioRepositorio repositorio,
                                          ServicioUsuario servicioUsuario,
                                          PasswordEncoder codificador,
                                          GeneradorPasswordTemporal generador,
                                          ServicioAuditoria auditoria) {
        this.repositorio = repositorio;
        this.servicioUsuario = servicioUsuario;
        this.codificador = codificador;
        this.generador = generador;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repositorio.findAllByOrderByNombreCompletoAsc();
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }

    @Transactional
    public UsuarioConPassword crear(FormularioUsuario formulario, Usuario administrador, String ip) {
        String username = formulario.getUsername().trim().toLowerCase(Locale.ROOT);
        if (repositorio.existsByUsername(username)) {
            throw new ReglaDeNegocioException("Ya existe un usuario «" + username + "». Elegí otro nombre de usuario.");
        }
        String temporal = generador.generar(username);
        Usuario usuario = servicioUsuario.crear(username, temporal, formulario.getNombreCompleto(), administrador.getId());

        auditoria.registrar(administrador.getId(), AccionAuditoria.USUARIO_CREADO, ServicioAuditoria.ENTIDAD_USUARIO,
                usuario.getId(), usuario.getNombreCompleto() + " (" + usuario.getUsername() + ")", ip);
        return new UsuarioConPassword(usuario, temporal);
    }

    /** Bloquea sin fecha de vencimiento: no puede entrar hasta que lo desbloqueen. */
    @Transactional
    public Usuario bloquear(Long id, Usuario administrador, String ip) {
        Usuario usuario = obtener(id);
        if (usuario.getId().equals(administrador.getId())) {
            throw new ReglaDeNegocioException("No podés bloquear tu propio usuario.");
        }
        if (!usuario.isActivo()) {
            throw new ReglaDeNegocioException("El usuario de " + usuario.getNombreCompleto() + " ya estaba bloqueado.");
        }
        if (usuario.getRol() == Rol.ADMIN && repositorio.countByActivoTrueAndRol(Rol.ADMIN) <= 1) {
            throw new ReglaDeNegocioException("No se puede bloquear al único administrador activo: nadie podría entrar.");
        }
        usuario.setActivo(false);
        auditoria.registrar(administrador.getId(), AccionAuditoria.USUARIO_BLOQUEADO, ServicioAuditoria.ENTIDAD_USUARIO,
                usuario.getId(), usuario.getNombreCompleto() + " (" + usuario.getUsername() + ")", ip);
        return usuario;
    }

    /** Quita cualquier bloqueo: el puesto por un administrador y el de intentos fallidos. */
    @Transactional
    public Usuario desbloquear(Long id, Usuario administrador, String ip) {
        Usuario usuario = obtener(id);
        if (usuario.isActivo() && !usuario.estaBloqueado()) {
            throw new ReglaDeNegocioException("El usuario de " + usuario.getNombreCompleto() + " no está bloqueado.");
        }
        usuario.setActivo(true);
        usuario.setBloqueadoHasta(null);
        usuario.setIntentosFallidos(0);
        auditoria.registrar(administrador.getId(), AccionAuditoria.USUARIO_DESBLOQUEADO, ServicioAuditoria.ENTIDAD_USUARIO,
                usuario.getId(), usuario.getNombreCompleto() + " (" + usuario.getUsername() + ")", ip);
        return usuario;
    }

    /**
     * Reemplaza la contraseña por una temporal nueva. La anterior deja de
     * funcionar en el acto, y la persona tendrá que cambiar la temporal al entrar.
     */
    @Transactional
    public UsuarioConPassword restablecerPassword(Long id, Usuario administrador, String ip) {
        Usuario usuario = obtener(id);
        if (usuario.getId().equals(administrador.getId())) {
            throw new ReglaDeNegocioException("Para cambiar tu propia contraseña usá «Mi contraseña», arriba a la derecha.");
        }
        String temporal = generador.generar(usuario.getUsername());
        usuario.setPasswordHash(codificador.encode(temporal));
        usuario.setDebeCambiarPassword(true);
        usuario.setBloqueadoHasta(null);
        usuario.setIntentosFallidos(0);
        auditoria.registrar(administrador.getId(), AccionAuditoria.PASSWORD_RESTABLECIDA, ServicioAuditoria.ENTIDAD_USUARIO,
                usuario.getId(), usuario.getNombreCompleto() + " (" + usuario.getUsername() + ")", ip);
        return new UsuarioConPassword(usuario, temporal);
    }
}
