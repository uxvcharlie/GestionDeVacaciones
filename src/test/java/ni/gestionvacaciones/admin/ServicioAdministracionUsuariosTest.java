package ni.gestionvacaciones.admin;

import ni.gestionvacaciones.PruebaConPostgres;
import ni.gestionvacaciones.auditoria.Auditoria;
import ni.gestionvacaciones.auditoria.AuditoriaRepositorio;
import ni.gestionvacaciones.auditoria.ServicioAuditoria;
import ni.gestionvacaciones.comun.ReglaDeNegocioException;
import ni.gestionvacaciones.seguridad.PoliticaPassword;
import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Panel de usuarios contra PostgreSQL real. */
class ServicioAdministracionUsuariosTest extends PruebaConPostgres {

    @Autowired
    private ServicioAdministracionUsuarios servicio;

    @Autowired
    private ServicioUsuario servicioUsuario;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private AuditoriaRepositorio auditoriaRepositorio;

    @Autowired
    private PasswordEncoder codificador;

    @Autowired
    private PoliticaPassword politica;

    @Test
    @DisplayName("Crear genera una contraseña temporal segura, obliga a cambiarla y queda en la bitácora")
    void crear() {
        ServicioAdministracionUsuarios.UsuarioConPassword creado =
                servicio.crear(formulario("Nueva Persona Administración", "Nueva.Persona"), brenda(), "10.0.0.5");

        Usuario usuario = usuarioRepositorio.findById(creado.usuario().getId()).orElseThrow();
        assertThat(usuario.getUsername()).isEqualTo("nueva.persona");
        assertThat(usuario.isDebeCambiarPassword()).isTrue();
        assertThat(usuario.getCreadoPor()).isEqualTo(brenda().getId());

        String temporal = creado.passwordTemporal();
        assertThat(temporal).matches("[a-z2-9]{4}-[a-z2-9]{4}-[a-z2-9]{4}");
        assertThat(politica.revisar(temporal, usuario.getUsername())).isEmpty();
        assertThat(codificador.matches(temporal, usuario.getPasswordHash())).isTrue();
        assertThat(usuario.getPasswordHash()).doesNotContain(temporal);

        assertThat(acciones(usuario)).contains("USUARIO_CREADO");
    }

    @Test
    @DisplayName("No se repiten nombres de usuario, sin importar mayúsculas")
    void usernameDuplicado() {
        servicio.crear(formulario("Persona Duplicada", "persona.duplicada"), brenda(), null);

        assertThatThrownBy(() -> servicio.crear(formulario("Otra Persona", "Persona.Duplicada"), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Ya existe un usuario");
    }

    @Test
    @DisplayName("Nadie se bloquea ni se restablece la contraseña a sí mismo desde el panel")
    void noASiMismo() {
        assertThatThrownBy(() -> servicio.bloquear(brenda().getId(), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("tu propio usuario");
        assertThatThrownBy(() -> servicio.restablecerPassword(brenda().getId(), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("Mi contraseña");
    }

    @Test
    @DisplayName("Bloquear y desbloquear; desbloquear también quita el bloqueo por intentos")
    void bloquearYDesbloquear() {
        Usuario usuario = servicio.crear(formulario("Persona Para Bloquear", "persona.bloquear"), brenda(), null).usuario();

        servicio.bloquear(usuario.getId(), brenda(), null);
        assertThat(recargar(usuario).isActivo()).isFalse();
        assertThatThrownBy(() -> servicio.bloquear(usuario.getId(), brenda(), null))
                .isInstanceOf(ReglaDeNegocioException.class);

        Usuario bloqueado = recargar(usuario);
        bloqueado.setBloqueadoHasta(Instant.now().plus(Duration.ofMinutes(10)));
        bloqueado.setIntentosFallidos(3);
        usuarioRepositorio.save(bloqueado);

        servicio.desbloquear(usuario.getId(), brenda(), null);
        Usuario desbloqueado = recargar(usuario);
        assertThat(desbloqueado.isActivo()).isTrue();
        assertThat(desbloqueado.getBloqueadoHasta()).isNull();
        assertThat(desbloqueado.getIntentosFallidos()).isZero();

        assertThat(acciones(usuario)).contains("USUARIO_BLOQUEADO", "USUARIO_DESBLOQUEADO");
    }

    @Test
    @DisplayName("Restablecer invalida la contraseña anterior y genera otra temporal")
    void restablecer() {
        Usuario usuario = servicioUsuario.crear("persona.restablecer", "la-contraseña-de-antes", "Persona Para Restablecer", null);
        usuario.setDebeCambiarPassword(false);
        usuario.setIntentosFallidos(2);
        usuarioRepositorio.save(usuario);

        String temporal = servicio.restablecerPassword(usuario.getId(), brenda(), null).passwordTemporal();

        Usuario guardado = recargar(usuario);
        assertThat(codificador.matches("la-contraseña-de-antes", guardado.getPasswordHash())).isFalse();
        assertThat(codificador.matches(temporal, guardado.getPasswordHash())).isTrue();
        assertThat(guardado.isDebeCambiarPassword()).isTrue();
        assertThat(guardado.getIntentosFallidos()).isZero();
        assertThat(acciones(usuario)).contains("PASSWORD_RESTABLECIDA");
    }

    private Usuario brenda() {
        return usuarioRepositorio.findByUsername("brenda").orElseThrow();
    }

    private Usuario recargar(Usuario usuario) {
        return usuarioRepositorio.findById(usuario.getId()).orElseThrow();
    }

    private java.util.List<String> acciones(Usuario usuario) {
        return auditoriaRepositorio.findByEntidadAndEntidadIdOrderByCreadoEnDescIdDesc(
                ServicioAuditoria.ENTIDAD_USUARIO, usuario.getId()).stream().map(Auditoria::getAccion).toList();
    }

    private static FormularioUsuario formulario(String nombre, String username) {
        FormularioUsuario formulario = new FormularioUsuario();
        formulario.setNombreCompleto(nombre);
        formulario.setUsername(username);
        return formulario;
    }
}
