package ni.gestionvacaciones.movil

import android.app.Application
import ni.gestionvacaciones.movil.acceso.AlmacenAcceso
import ni.gestionvacaciones.movil.acceso.ControlSesion
import ni.gestionvacaciones.movil.datos.BaseDeDatos
import ni.gestionvacaciones.movil.datos.Repositorio

/**
 * Lo que existe una sola vez mientras la app está abierta: la base de datos,
 * el repositorio y el control del PIN.
 *
 * Android crea esta clase antes que cualquier pantalla (está declarada en el
 * AndroidManifest). Las pantallas la consiguen con `LocalContenedor.current`.
 */
class AppVacaciones : Application() {

    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        val db = BaseDeDatos.crear(this)
        val repositorio = Repositorio(db)
        contenedor = Contenedor(
            repositorio = repositorio,
            acceso = AlmacenAcceso(this),
            sesion = ControlSesion(),
        )
    }
}

class Contenedor(
    val repositorio: Repositorio,
    val acceso: AlmacenAcceso,
    val sesion: ControlSesion,
)
