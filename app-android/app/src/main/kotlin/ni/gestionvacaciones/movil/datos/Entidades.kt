package ni.gestionvacaciones.movil.datos

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ni.gestionvacaciones.movil.dominio.AccionBitacora
import ni.gestionvacaciones.movil.dominio.EstadoSolicitud
import ni.gestionvacaciones.movil.dominio.TipoMovimiento
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import java.time.Instant
import java.time.LocalDate

/*
 * Las tablas de la base de datos que vive DENTRO del celular.
 *
 * Es el mismo esquema que la web (V1__esquema_inicial.sql), con dos cambios:
 *   - No hay tabla de usuarios: a la app entra una sola persona, con su PIN.
 *   - Por eso desaparecen las columnas "registrado_por", "anulado_por" y
 *     "realizado_por": siempre es la misma persona.
 *
 * Regla de oro, igual que en la web: TODO el tiempo se guarda en MINUTOS
 * enteros. Nunca en días decimales.
 */

/** Un trabajador del que se lleva el control. Nunca se borra: se da de baja. */
@Entity(tableName = "funcionario", indices = [Index("activo")])
data class FuncionarioEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nombre_completo") val nombreCompleto: String,
    val cargo: String? = null,
    @ColumnInfo(name = "area_o_dependencia") val areaODependencia: String? = null,
    @ColumnInfo(name = "fecha_ingreso") val fechaIngreso: LocalDate? = null,
    /**
     * SALDO ÚNICO en minutos. De acá descuentan los tres tipos: vacaciones,
     * citas médicas y permisos. Solo queda negativo con autorización expresa.
     * Nunca se cambia sin escribir también su movimiento.
     */
    @ColumnInfo(name = "saldo_minutos") val saldoMinutos: Int = 0,
    /** false = dado de baja. Su historial se conserva completo. */
    val activo: Boolean = true,
    @ColumnInfo(name = "creado_en") val creadoEn: Instant,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: Instant,
)

/** Cada vacación, cita médica o permiso que se anota. No se borra: se anula. */
@Entity(
    tableName = "solicitud",
    foreignKeys = [ForeignKey(entity = FuncionarioEntidad::class, parentColumns = ["id"], childColumns = ["funcionario_id"])],
    indices = [Index("funcionario_id"), Index("fecha_inicio", "fecha_fin"), Index("estado")],
)
data class SolicitudEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "funcionario_id") val funcionarioId: Long,
    val tipo: TipoSolicitud,
    @ColumnInfo(name = "fecha_inicio") val fechaInicio: LocalDate,
    @ColumnInfo(name = "fecha_fin") val fechaFin: LocalDate,
    val minutos: Int,
    /** Texto corto y opcional. En citas médicas NUNCA debe tener información médica. */
    val motivo: String? = null,
    val estado: EstadoSolicitud = EstadoSolicitud.REGISTRADA,
    @ColumnInfo(name = "creado_en") val creadoEn: Instant,
    @ColumnInfo(name = "anulado_en") val anuladoEn: Instant? = null,
    @ColumnInfo(name = "motivo_anulacion") val motivoAnulacion: String? = null,
)

/**
 * El libro contable del saldo. Cada cambio deja su renglón, así que siempre se
 * puede reconstruir la historia: el saldo de un funcionario es SIEMPRE la suma
 * de todos sus movimientos.
 */
@Entity(
    tableName = "movimiento_saldo",
    foreignKeys = [
        ForeignKey(entity = FuncionarioEntidad::class, parentColumns = ["id"], childColumns = ["funcionario_id"]),
        ForeignKey(entity = SolicitudEntidad::class, parentColumns = ["id"], childColumns = ["solicitud_id"]),
    ],
    indices = [Index("funcionario_id"), Index("solicitud_id")],
)
data class MovimientoEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "funcionario_id") val funcionarioId: Long,
    @ColumnInfo(name = "solicitud_id") val solicitudId: Long? = null,
    val tipo: TipoMovimiento,
    /** Positivo suma, negativo resta. Nunca cero. */
    val minutos: Int,
    /** Cómo quedó el saldo DESPUÉS de este movimiento. */
    @ColumnInfo(name = "saldo_resultante") val saldoResultante: Int,
    val descripcion: String? = null,
    @ColumnInfo(name = "creado_en") val creadoEn: Instant,
)

/** Qué se hizo y cuándo. */
@Entity(tableName = "bitacora", indices = [Index("creado_en")])
data class BitacoraEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accion: AccionBitacora,
    val detalle: String? = null,
    @ColumnInfo(name = "creado_en") val creadoEn: Instant,
)

/** Ajustes editables desde la app: horas por jornada, zona horaria, nombre. */
@Entity(tableName = "parametro")
data class ParametroEntidad(
    @PrimaryKey val clave: String,
    val valor: String,
)

/** Una solicitud junto con los datos del funcionario, para listas y reportes. */
data class SolicitudConFuncionario(
    @Embedded val solicitud: SolicitudEntidad,
    @ColumnInfo(name = "nombre_funcionario") val nombreFuncionario: String,
    @ColumnInfo(name = "cargo_funcionario") val cargoFuncionario: String?,
    @ColumnInfo(name = "area_funcionario") val areaFuncionario: String?,
)
