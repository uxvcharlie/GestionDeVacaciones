package ni.gestionvacaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Toda la configuración de seguridad de la aplicación, en un solo archivo.
 *
 * <p>Está comentada línea por línea a propósito: es el archivo donde un error
 * silencioso cuesta más caro.</p>
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    /**
     * Cómo se guardan y comparan las contraseñas.
     *
     * <p>BCrypt con costo 12. El "costo" es cuántas vueltas de cálculo hace:
     * cada +1 duplica el tiempo. 12 significa que verificar una contraseña
     * tarda unos 0,25 segundos, algo imperceptible para Brenda pero carísimo
     * para alguien que quiera probar millones de contraseñas por segundo.</p>
     *
     * <p>BCrypt además le agrega a cada contraseña una "sal" aleatoria, así que
     * dos personas con la misma contraseña tienen hashes distintos.</p>
     */
    @Bean
    public PasswordEncoder codificadorDePasswords() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
            // -----------------------------------------------------------------
            // Quién puede entrar a qué
            // -----------------------------------------------------------------
            .authorizeHttpRequests(peticiones -> peticiones
                // Archivos públicos: la hoja de estilos, el JavaScript, el ícono.
                .requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll()
                // La pantalla de ingreso tiene que ser pública, obviamente.
                .requestMatchers("/ingresar").permitAll()
                // Página de error propia.
                .requestMatchers("/error").permitAll()
                // Render consulta esta dirección para saber si la app está viva.
                .requestMatchers("/actuator/health").permitAll()
                // TODO lo demás exige haber ingresado. Esta línea va de última
                // y es la que garantiza que no se nos escape ninguna pantalla.
                .anyRequest().authenticated()
            )

            // -----------------------------------------------------------------
            // Formulario de ingreso
            // -----------------------------------------------------------------
            .formLogin(ingreso -> ingreso
                .loginPage("/ingresar")            // nuestra pantalla, no la de Spring
                .loginProcessingUrl("/ingresar")   // a dónde manda el formulario
                .usernameParameter("usuario")
                .passwordParameter("clave")
                .defaultSuccessUrl("/", true)      // siempre al tablero
                .failureUrl("/ingresar?error")
                .permitAll()
            )

            // -----------------------------------------------------------------
            // Cierre de sesión
            // -----------------------------------------------------------------
            .logout(salida -> salida
                // Por POST y con token CSRF: así nadie puede cerrarle la sesión
                // a Brenda con un simple enlace.
                .logoutUrl("/salir")
                .logoutSuccessUrl("/ingresar?salida")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("SESION")
                .permitAll()
            )

            // -----------------------------------------------------------------
            // Sesiones
            // -----------------------------------------------------------------
            .sessionManagement(sesion -> sesion
                // Al ingresar se crea una sesión NUEVA y se descarta la anterior.
                // Esto evita el ataque de "fijación de sesión": que alguien te
                // pase un enlace con un identificador de sesión que él ya conoce.
                .sessionFixation(fijacion -> fijacion.newSession())
                // Si la sesión venció (30 minutos sin actividad), se vuelve al
                // login con un aviso, no con un error.
                .invalidSessionUrl("/ingresar?expirada")
            )

            // -----------------------------------------------------------------
            // Cabeceras de seguridad que se mandan en cada respuesta
            // -----------------------------------------------------------------
            .headers(cabeceras -> cabeceras
                // Nadie puede meter esta aplicación dentro de un <iframe>.
                // Protege contra "clickjacking".
                .frameOptions(marco -> marco.deny())

                // HSTS: una vez que el navegador visitó el sitio por HTTPS, se
                // niega a volver por HTTP durante un año. Solo tiene efecto en
                // producción, que es donde hay HTTPS.
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000L)
                )

                // No filtrar a otros sitios qué página estaba viendo la usuaria.
                .referrerPolicy(politica -> politica
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)
                )

                // Content-Security-Policy: le dice al navegador de dónde puede
                // cargar cosas. 'self' = solo desde esta misma aplicación.
                // No hay 'unsafe-inline': por eso todo el JavaScript va en
                // archivos y no incrustado en el HTML. Si alguien lograra
                // inyectar un <script> en una página, el navegador lo ignora.
                .contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
                    "default-src 'self'",
                    "script-src 'self'",
                    "style-src 'self'",
                    "img-src 'self' data:",
                    "font-src 'self'",
                    "connect-src 'self'",
                    "form-action 'self'",
                    "frame-ancestors 'none'",
                    "base-uri 'self'",
                    "object-src 'none'"
                )))
            );

        // NOTA: la protección CSRF viene activada por omisión en Spring Security
        // y NO la tocamos. Thymeleaf agrega el token automáticamente a todo
        // formulario que use th:action.

        // X-Content-Type-Options: nosniff también viene activado por omisión.

        return http.build();
    }
}
