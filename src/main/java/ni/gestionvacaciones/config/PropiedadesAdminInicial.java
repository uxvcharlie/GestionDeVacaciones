package ni.gestionvacaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Los datos del administrador inicial, leídos de las variables de entorno
 * ADMIN_USERNAME, ADMIN_PASSWORD_INICIAL y ADMIN_NOMBRE.
 *
 * <p>Se usan una sola vez, en el primer arranque. Nunca hay una contraseña por
 * omisión escrita en el código: si las variables no están, la aplicación no
 * arranca y explica qué falta.</p>
 */
@ConfigurationProperties(prefix = "app.admin-inicial")
public record PropiedadesAdminInicial(String username, String password, String nombre) {

    public boolean estaCompleto() {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank()
                && nombre != null && !nombre.isBlank();
    }
}
