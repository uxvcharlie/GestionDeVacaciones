package ni.gestionvacaciones.movil.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/*
 * Las consultas a la base. Room revisa CADA consulta contra las tablas al
 * compilar: si una columna está mal escrita, la app no compila. Es el mismo
 * espíritu que "ddl-auto: validate" en la web.
 *
 * "Flow" = la consulta se vuelve a ejecutar sola cada vez que cambian los
 * datos, y la pantalla se actualiza sin hacer nada más.
 *
 * Fíjate que no hay ningún DELETE fuera del respaldo: funcionarios y
 * solicitudes nunca se borran.
 */

@Dao
interface FuncionarioDao {
    @Query("SELECT * FROM funcionario")
    fun observarTodos(): Flow<List<FuncionarioEntidad>>

    @Query("SELECT * FROM funcionario")
    suspend fun todos(): List<FuncionarioEntidad>

    @Query("SELECT * FROM funcionario WHERE id = :id")
    fun observar(id: Long): Flow<FuncionarioEntidad?>

    @Query("SELECT * FROM funcionario WHERE id = :id")
    suspend fun obtener(id: Long): FuncionarioEntidad?

    @Insert
    suspend fun insertar(funcionario: FuncionarioEntidad): Long

    @Update
    suspend fun actualizar(funcionario: FuncionarioEntidad)
}

@Dao
interface SolicitudDao {
    @Query("SELECT * FROM solicitud WHERE funcionario_id = :funcionarioId ORDER BY fecha_inicio DESC, id DESC")
    fun observarDeFuncionario(funcionarioId: Long): Flow<List<SolicitudEntidad>>

    @Query("SELECT * FROM solicitud WHERE id = :id")
    suspend fun obtener(id: Long): SolicitudEntidad?

    @Query("SELECT * FROM solicitud")
    suspend fun todas(): List<SolicitudEntidad>

    @Insert
    suspend fun insertar(solicitud: SolicitudEntidad): Long

    @Update
    suspend fun actualizar(solicitud: SolicitudEntidad)

    /** Otras solicitudes vigentes del mismo funcionario que se cruzan con [inicio, fin]. */
    @Query(
        "SELECT * FROM solicitud WHERE funcionario_id = :funcionarioId AND estado = 'REGISTRADA' " +
            "AND fecha_inicio <= :fin AND fecha_fin >= :inicio ORDER BY fecha_inicio",
    )
    suspend fun traslapes(funcionarioId: Long, inicio: LocalDate, fin: LocalDate): List<SolicitudEntidad>

    /** Las últimas solicitudes registradas o anuladas, con el nombre del funcionario. */
    @Query(
        "SELECT s.*, f.nombre_completo AS nombre_funcionario, f.cargo AS cargo_funcionario, " +
            "f.area_o_dependencia AS area_funcionario " +
            "FROM solicitud s JOIN funcionario f ON f.id = s.funcionario_id " +
            "ORDER BY s.creado_en DESC, s.id DESC LIMIT :limite",
    )
    fun observarRecientes(limite: Int): Flow<List<SolicitudConFuncionario>>

    /** Solicitudes que EMPIEZAN dentro del período, también las anuladas. */
    @Query(
        "SELECT s.*, f.nombre_completo AS nombre_funcionario, f.cargo AS cargo_funcionario, " +
            "f.area_o_dependencia AS area_funcionario " +
            "FROM solicitud s JOIN funcionario f ON f.id = s.funcionario_id " +
            "WHERE s.fecha_inicio BETWEEN :inicio AND :fin " +
            "ORDER BY s.fecha_inicio, f.nombre_completo, s.id",
    )
    fun observarEnPeriodo(inicio: LocalDate, fin: LocalDate): Flow<List<SolicitudConFuncionario>>

    @Query(
        "SELECT s.*, f.nombre_completo AS nombre_funcionario, f.cargo AS cargo_funcionario, " +
            "f.area_o_dependencia AS area_funcionario " +
            "FROM solicitud s JOIN funcionario f ON f.id = s.funcionario_id " +
            "WHERE s.fecha_inicio BETWEEN :inicio AND :fin " +
            "ORDER BY s.fecha_inicio, f.nombre_completo, s.id",
    )
    suspend fun enPeriodo(inicio: LocalDate, fin: LocalDate): List<SolicitudConFuncionario>
}

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimiento_saldo WHERE funcionario_id = :funcionarioId ORDER BY creado_en DESC, id DESC")
    fun observarDeFuncionario(funcionarioId: Long): Flow<List<MovimientoEntidad>>

    @Query("SELECT * FROM movimiento_saldo WHERE funcionario_id = :funcionarioId ORDER BY creado_en DESC, id DESC")
    suspend fun deFuncionario(funcionarioId: Long): List<MovimientoEntidad>

    @Query("SELECT * FROM movimiento_saldo")
    suspend fun todos(): List<MovimientoEntidad>

    @Insert
    suspend fun insertar(movimiento: MovimientoEntidad): Long
}

@Dao
interface BitacoraDao {
    @Query("SELECT * FROM bitacora ORDER BY creado_en DESC, id DESC")
    fun observarTodo(): Flow<List<BitacoraEntidad>>

    @Query("SELECT * FROM bitacora")
    suspend fun todo(): List<BitacoraEntidad>

    @Insert
    suspend fun insertar(entrada: BitacoraEntidad): Long
}

@Dao
interface ParametroDao {
    @Query("SELECT * FROM parametro")
    fun observarTodos(): Flow<List<ParametroEntidad>>

    @Query("SELECT * FROM parametro")
    suspend fun todos(): List<ParametroEntidad>

    @Upsert
    suspend fun guardar(parametro: ParametroEntidad)
}

/** Solo lo usa la restauración de un respaldo, dentro de una transacción. */
@Dao
interface RespaldoDao {
    @Query("DELETE FROM movimiento_saldo") suspend fun vaciarMovimientos()
    @Query("DELETE FROM solicitud") suspend fun vaciarSolicitudes()
    @Query("DELETE FROM funcionario") suspend fun vaciarFuncionarios()
    @Query("DELETE FROM bitacora") suspend fun vaciarBitacora()
    @Query("DELETE FROM parametro") suspend fun vaciarParametros()

    @Insert suspend fun insertarFuncionarios(lista: List<FuncionarioEntidad>)
    @Insert suspend fun insertarSolicitudes(lista: List<SolicitudEntidad>)
    @Insert suspend fun insertarMovimientos(lista: List<MovimientoEntidad>)
    @Insert suspend fun insertarBitacora(lista: List<BitacoraEntidad>)
    @Insert suspend fun insertarParametros(lista: List<ParametroEntidad>)
}
