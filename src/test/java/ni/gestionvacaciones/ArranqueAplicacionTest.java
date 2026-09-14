package ni.gestionvacaciones;

import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de humo: la aplicación arranca, Flyway crea el esquema y se crea el
 * administrador inicial. Si una entidad dejara de coincidir con su tabla,
 * {@code ddl-auto: validate} haría fallar el arranque y esta prueba.
 */
class ArranqueAplicacionTest extends PruebaConPostgres {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Test
    @DisplayName("La aplicación arranca y Flyway crea el esquema completo")
    void laAplicacionArranca() {
        assertThat(usuarioRepositorio).isNotNull();
    }

    @Test
    @DisplayName("Se crea el administrador inicial y queda obligado a cambiar su contraseña")
    void seCreaElAdministradorInicial() {
        Optional<Usuario> admin = usuarioRepositorio.findByUsername("brenda");

        assertThat(admin).isPresent();
        assertThat(admin.get().getNombreCompleto()).isEqualTo("Brenda Vásquez");
        assertThat(admin.get().isDebeCambiarPassword()).isTrue();
        assertThat(admin.get().isActivo()).isTrue();
    }

    @Test
    @DisplayName("La contraseña nunca se guarda en texto plano")
    void laPasswordSeGuardaComoHash() {
        Usuario admin = usuarioRepositorio.findByUsername("brenda").orElseThrow();

        assertThat(admin.getPasswordHash()).isNotEqualTo("frase-larga-de-prueba");
        // Un hash de BCrypt con costo 12 siempre empieza así.
        assertThat(admin.getPasswordHash()).startsWith("$2a$12$");
    }
}
