package ni.gestionvacaciones.config;

import ni.gestionvacaciones.seguridad.ServicioUsuario;
import ni.gestionvacaciones.seguridad.Usuario;
import ni.gestionvacaciones.seguridad.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario administrador la primera vez que arranca la aplicación.
 *
 * <p>Condición: solo actúa si la tabla de usuarios está <b>completamente
 * vacía</b>. En cualquier arranque posterior no hace nada, así que no puede
 * sobrescribir la contraseña que Brenda ya eligió.</p>
 */
@Component
public class CreadorAdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CreadorAdminInicial.class);

    private final UsuarioRepositorio repositorio;
    private final ServicioUsuario servicioUsuario;
    private final PropiedadesAdminInicial propiedades;

    public CreadorAdminInicial(UsuarioRepositorio repositorio,
                               ServicioUsuario servicioUsuario,
                               PropiedadesAdminInicial propiedades) {
        this.repositorio = repositorio;
        this.servicioUsuario = servicioUsuario;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.count() > 0) {
            log.info("Ya existen usuarios registrados: no se crea ningún administrador inicial.");
            return;
        }

        if (!propiedades.estaCompleto()) {
            // Una aplicación sin ningún usuario es una aplicación a la que
            // nadie puede entrar. Preferimos fallar acá, con un mensaje claro,
            // antes que dejarla arrancar rota.
            throw new IllegalStateException("""
                    No hay ningún usuario en la base de datos y faltan las variables de \
                    entorno para crear el administrador inicial.

                    Definí estas tres antes de arrancar:
                      ADMIN_USERNAME          (por ejemplo: brenda)
                      ADMIN_PASSWORD_INICIAL  (12 caracteres o más)
                      ADMIN_NOMBRE            (por ejemplo: Brenda Vásquez)

                    En tu computadora podés ponerlas en el archivo .env y usar ./ejecutar-local.sh
                    """);
        }

        try {
            Usuario admin = servicioUsuario.crear(
                    propiedades.username(),
                    propiedades.password(),
                    propiedades.nombre(),
                    null); // lo crea el sistema, no otra persona

            log.info("Administrador inicial creado: {} ({}). "
                            + "En su primer ingreso el sistema le va a exigir cambiar la contraseña.",
                    admin.getUsername(), admin.getNombreCompleto());

        } catch (IllegalArgumentException e) {
            // Por ejemplo: la contraseña inicial es muy corta o muy obvia.
            throw new IllegalStateException(
                    "No se pudo crear el administrador inicial. " + e.getMessage(), e);
        }
    }
}
