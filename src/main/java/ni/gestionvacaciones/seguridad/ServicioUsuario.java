package ni.gestionvacaciones.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Operaciones sobre usuarios del sistema.
 *
 * <p>En esta fase solo necesitamos dos cosas: crear el administrador inicial y
 * cambiar la contraseña. El panel completo de usuarios llega en la Fase 5.</p>
 */
@Service
public class ServicioUsuario {

    private final UsuarioRepositorio repositorio;
    private final PasswordEncoder codificador;
    private final PoliticaPassword politica;

    public ServicioUsuario(UsuarioRepositorio repositorio,
                           PasswordEncoder codificador,
                           PoliticaPassword politica) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.politica = politica;
    }

    /**
     * Busca un usuario por su nombre de usuario, normalizando a minúsculas.
     */
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return repositorio.findByUsername(username.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Crea un usuario. La contraseña se guarda siempre como hash.
     *
     * @param creadoPor id del usuario que lo está creando, o {@code null} si lo
     *                  crea el sistema en el primer arranque
     */
    @Transactional
    public Usuario crear(String username, String passwordEnClaro, String nombreCompleto, Long creadoPor) {
        String usuarioNormalizado = username.trim().toLowerCase(Locale.ROOT);

        politica.revisar(passwordEnClaro, usuarioNormalizado).ifPresent(problema -> {
            throw new IllegalArgumentException(problema);
        });
        if (repositorio.existsByUsername(usuarioNormalizado)) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre " + usuarioNormalizado + ".");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(usuarioNormalizado);
        usuario.setPasswordHash(codificador.encode(passwordEnClaro));
        usuario.setNombreCompleto(nombreCompleto.trim());
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(true);
        // Siempre TRUE: quien recibe una contraseña creada por otra persona
        // está obligado a cambiarla en su primer ingreso.
        usuario.setDebeCambiarPassword(true);
        usuario.setCreadoPor(creadoPor);
        return repositorio.save(usuario);
    }

    /**
     * Cambia la contraseña de un usuario.
     *
     * @return vacío si salió bien; si no, el mensaje en español para la pantalla
     */
    @Transactional
    public Optional<String> cambiarPassword(String username, String actual, String nueva, String confirmacion) {
        Optional<Usuario> encontrado = buscarPorUsername(username);
        if (encontrado.isEmpty()) {
            return Optional.of("No pudimos identificar tu usuario. Volvé a ingresar.");
        }
        Usuario usuario = encontrado.get();

        if (!codificador.matches(actual, usuario.getPasswordHash())) {
            return Optional.of("Tu contraseña actual no es correcta.");
        }
        if (!nueva.equals(confirmacion)) {
            return Optional.of("La contraseña nueva y su confirmación no son iguales.");
        }
        if (codificador.matches(nueva, usuario.getPasswordHash())) {
            return Optional.of("La contraseña nueva tiene que ser distinta de la actual.");
        }
        Optional<String> problema = politica.revisar(nueva, usuario.getUsername());
        if (problema.isPresent()) {
            return problema;
        }

        usuario.setPasswordHash(codificador.encode(nueva));
        usuario.setDebeCambiarPassword(false);
        repositorio.save(usuario);
        return Optional.empty();
    }
}
