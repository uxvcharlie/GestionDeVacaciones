package ni.gestionvacaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de la aplicación.
 *
 * <p>{@code @SpringBootApplication} le dice a Spring: buscá clases de
 * configuración, controladores y servicios dentro de este paquete y de todos
 * los que cuelgan de él.</p>
 *
 * <p>{@code @ConfigurationPropertiesScan} habilita las clases que leen
 * configuración desde application.yml, como {@code PropiedadesAdminInicial}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GestionVacacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionVacacionesApplication.class, args);
    }
}
