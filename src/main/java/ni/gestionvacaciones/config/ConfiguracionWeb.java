package ni.gestionvacaciones.config;

import ni.gestionvacaciones.web.InterceptorCambioPassword;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC.
 *
 * <p>Por ahora solo registra el interceptor que obliga a cambiar la contraseña.
 * En fases siguientes acá se agregan los formatos de fecha dd/MM/yyyy y la zona
 * horaria de Managua.</p>
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final InterceptorCambioPassword interceptorCambioPassword;

    public ConfiguracionWeb(InterceptorCambioPassword interceptorCambioPassword) {
        this.interceptorCambioPassword = interceptorCambioPassword;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(interceptorCambioPassword)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/favicon.ico", "/error", "/actuator/**");
    }
}
