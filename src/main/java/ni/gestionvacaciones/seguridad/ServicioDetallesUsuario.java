package ni.gestionvacaciones.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Le dice a Spring Security dónde buscar los usuarios: en nuestra tabla.
 *
 * <p>Spring llama a este servicio cada vez que alguien intenta entrar. Nosotros
 * solo devolvemos el usuario; comparar la contraseña con el hash lo hace Spring
 * usando el {@code PasswordEncoder} configurado.</p>
 */
@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final UsuarioRepositorio repositorio;

    public ServicioDetallesUsuario(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizado = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return repositorio.findByUsername(normalizado)
                .map(UsuarioDetalles::new)
                // El mensaje es genérico a propósito: nunca le confirmamos a
                // nadie si un nombre de usuario existe o no.
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
}
