package ni.gestionvacaciones;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de todas las pruebas que necesitan la aplicación completa y una base de
 * datos PostgreSQL de verdad.
 *
 * <p>El contenedor se arranca UNA sola vez para todas las clases de prueba (el
 * bloque {@code static}), y Spring reutiliza el mismo contexto entre ellas
 * porque la configuración es idéntica. Sin esto, cada clase levantaría su
 * propio PostgreSQL y las pruebas tardarían mucho más.</p>
 *
 * <p>Como todas comparten la base, cada prueba usa nombres propios y nunca
 * depende de cuántos registros hay en total.</p>
 */
@SpringBootTest(properties = {
        // El administrador inicial se crea igual que en producción. Estos datos
        // son de mentira y solo existen dentro de las pruebas.
        "app.admin-inicial.username=brenda",
        "app.admin-inicial.password=frase-larga-de-prueba",
        "app.admin-inicial.nombre=Brenda Vásquez"
})
public abstract class PruebaConPostgres {

    /** Misma versión de PostgreSQL que en desarrollo y producción. */
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
