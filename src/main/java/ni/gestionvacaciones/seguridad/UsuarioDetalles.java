package ni.gestionvacaciones.seguridad;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Traductor entre nuestro {@link Usuario} y lo que Spring Security entiende.
 *
 * <p>Spring Security no conoce nuestra tabla: solo sabe pedir "dame el usuario
 * tal" y recibir un {@link UserDetails}. Esta clase envuelve nuestro usuario y
 * responde esas preguntas.</p>
 */
public class UsuarioDetalles implements UserDetails {

    private final Usuario usuario;

    public UsuarioDetalles(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(usuario.getRol().comoAutoridad()));
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    /**
     * Un usuario dado de baja no puede entrar.
     * Spring lanza DisabledException si esto devuelve false.
     */
    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }

    /**
     * Un usuario bloqueado por intentos fallidos no puede entrar hasta que
     * pase el tiempo de bloqueo. Spring lanza LockedException.
     */
    @Override
    public boolean isAccountNonLocked() {
        return !usuario.estaBloqueado();
    }
}
