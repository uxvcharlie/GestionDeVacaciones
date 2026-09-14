package ni.gestionvacaciones.config;

import ni.gestionvacaciones.web.InterceptorEstadoUsuario;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC.
 *
 * <p>Registra el interceptor que, en cada pantalla, cierra la sesión de un
 * usuario bloqueado y obliga a cambiar la contraseña temporal.</p>
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final InterceptorEstadoUsuario interceptorEstadoUsuario;

    public ConfiguracionWeb(InterceptorEstadoUsuario interceptorEstadoUsuario) {
        this.interceptorEstadoUsuario = interceptorEstadoUsuario;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(interceptorEstadoUsuario)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/favicon.ico", "/error", "/actuator/**");
    }
}
