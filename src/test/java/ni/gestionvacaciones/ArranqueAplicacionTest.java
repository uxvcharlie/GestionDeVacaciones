package ni.gestionvacaciones;

import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de humo de la Fase 1.
 *
 * <p>Levanta un PostgreSQL de verdad (descartable) con Testcontainers, aplica
 * las migraciones de Flyway y arranca la aplicación completa. Si el esquema y
 * las entidades no coincidieran, {@code ddl-auto: validate} haría fallar esta
 * prueba: es exactamente para eso.</p>
 */
@SpringBootTest(properties = {
        // El administrador inicial se crea desde variables de configuración,
        // igual que en producción. Estas son de mentira y solo viven acá.
        "app.admin-inicial.username=brenda",
        "app.admin-inicial.password=frase-larga-de-prueba",
        "app.admin-inicial.nombre=Brenda Vásquez"
})
@Testcontainers
class ArranqueAplicacionTest {

    /**
     * Misma versión de PostgreSQL que usamos en desarrollo y en producción.
     * {@code @ServiceConnection} se encarga de apuntar la aplicación a este
     * contenedor: no hay que configurar la URL a mano.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

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
