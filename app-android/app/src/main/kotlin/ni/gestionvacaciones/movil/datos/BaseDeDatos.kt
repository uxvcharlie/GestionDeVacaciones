package ni.gestionvacaciones.movil.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * La base de datos SQLite que vive dentro del celular, en el espacio privado
 * de la app: ninguna otra app puede leerla.
 *
 * "version = 1": si algún día cambia el esquema, se sube la versión y se
 * escribe una migración, igual que con Flyway. Room guarda una foto de cada
 * versión en app/esquemas/ para poder comparar.
 */
@Database(
    entities = [
        FuncionarioEntidad::class,
        SolicitudEntidad::class,
        MovimientoEntidad::class,
        BitacoraEntidad::class,
        ParametroEntidad::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class BaseDeDatos : RoomDatabase() {
    abstract fun funcionarios(): FuncionarioDao
    abstract fun solicitudes(): SolicitudDao
    abstract fun movimientos(): MovimientoDao
    abstract fun bitacora(): BitacoraDao
    abstract fun parametros(): ParametroDao
    abstract fun respaldo(): RespaldoDao

    companion object {
        const val NOMBRE_ARCHIVO = "vacaciones.db"

        fun crear(contexto: Context): BaseDeDatos =
            Room.databaseBuilder(contexto, BaseDeDatos::class.java, NOMBRE_ARCHIVO).build()
    }
}
