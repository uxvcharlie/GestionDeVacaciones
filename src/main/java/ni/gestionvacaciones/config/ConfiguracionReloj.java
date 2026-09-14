package ni.gestionvacaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * El reloj del sistema, como un componente más.
 *
 * <p>En vez de llamar a {@code LocalDateTime.now()} por todos lados, quien
 * necesita la hora pide este reloj. Así, en una prueba se puede reemplazar por
 * uno fijo y comprobar, por ejemplo, qué saludo sale a las 11:59.</p>
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
