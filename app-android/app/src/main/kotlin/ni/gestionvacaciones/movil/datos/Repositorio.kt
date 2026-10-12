package ni.gestionvacaciones.movil.datos

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ni.gestionvacaciones.movil.dominio.AccionBitacora
import ni.gestionvacaciones.movil.dominio.BorradorAjuste
import ni.gestionvacaciones.movil.dominio.BorradorFuncionario
import ni.gestionvacaciones.movil.dominio.BorradorSolicitud
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.EstadoSolicitud
import ni.gestionvacaciones.movil.dominio.EvaluacionSolicitud
import ni.gestionvacaciones.movil.dominio.EvaluadorSolicitud
import ni.gestionvacaciones.movil.dominio.FuncionarioEvaluado
import ni.gestionvacaciones.movil.dominio.ReglaDeNegocioException
import ni.gestionvacaciones.movil.dominio.ReglasDeAjuste
import ni.gestionvacaciones.movil.dominio.ReglasDeFuncionario
import ni.gestionvacaciones.movil.dominio.ReglasDeSaldo
import ni.gestionvacaciones.movil.dominio.SolicitudTraslapada
import ni.gestionvacaciones.movil.dominio.Texto
import ni.gestionvacaciones.movil.dominio.TipoMovimiento
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import java.text.Collator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/** Los ajustes de la app, ya leídos y con sus valores por omisión. */
data class Parametros(
    val horasPorJornada: Int = JORNADA_POR_OMISION,
    val zona: ZoneId = ZONA_POR_OMISION,
    val nombreUsuaria: String = "",
    val ultimoRespaldo: Instant? = null,
) {
    companion object {
        const val JORNADA_POR_OMISION = 8
        val ZONA_POR_OMISION: ZoneId = ZoneId.of("America/Managua")

        const val CLAVE_JORNADA = "horas_por_jornada"
        const val CLAVE_ZONA = "zona_horaria"
        const val CLAVE_NOMBRE = "nombre_usuaria"
        const val CLAVE_ULTIMO_RESPALDO = "ultimo_respaldo"

        fun desde(lista: List<ParametroEntidad>): Parametros {
            val mapa = lista.associate { it.clave to it.valor }
            return Parametros(
                horasPorJornada = mapa[CLAVE_JORNADA]?.toIntOrNull()?.takeIf { it in 1..24 } ?: JORNADA_POR_OMISION,
                zona = mapa[CLAVE_ZONA]?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZONA_POR_OMISION,
                nombreUsuaria = mapa[CLAVE_NOMBRE].orEmpty(),
                ultimoRespaldo = mapa[CLAVE_ULTIMO_RESPALDO]?.toLongOrNull()?.let(Instant::ofEpochMilli),
            )
        }
    }
}

/** Cómo quedó el saldo después de una operación, en minutos. */
data class CambioDeSaldo(val saldoAntes: Int, val saldoDespues: Int)

/** Cuántas solicitudes y cuánto tiempo, de un tipo. */
data class ResumenTipo(val cantidad: Int = 0, val minutos: Int = 0)

/**
 * El único lugar de la app que lee y escribe la base de datos.
 *
 * ## La regla más importante
 * Cada cambio de saldo escribe, en la MISMA transacción, el nuevo saldo y su
 * renglón en el libro de movimientos. O se guardan los dos, o ninguno: nunca
 * puede quedar un saldo cambiado sin su rastro. Por eso el saldo de cualquier
 * funcionario es siempre igual a la suma de sus movimientos.
 *
 * ## Las reglas no viven acá
 * Este repositorio solo guarda. Las reglas (cuándo se puede, qué mensaje
 * mostrar) están en el paquete `dominio`, que no sabe nada de Android y tiene
 * sus propias pruebas. Acá se vuelven a aplicar justo antes de guardar: la
 * pantalla nunca es la única barrera.
 *
 * @param reloj de dónde sale "ahora"; las pruebas lo reemplazan por uno fijo.
 */
class Repositorio(
    private val db: BaseDeDatos,
    private val reloj: () -> Instant = Instant::now,
) {
    private val ordenEspanol: Collator = Collator.getInstance(Locale.forLanguageTag("es"))

    // =========================================================================
    //  Parámetros
    // =========================================================================

    val parametros: Flow<Parametros> = db.parametros().observarTodos().map(Parametros::desde)

    suspend fun parametrosActuales(): Parametros = Parametros.desde(db.parametros().todos())

    fun ahora(): Instant = reloj()

    /** La fecha de hoy en la zona horaria configurada (Managua), no la del celular. */
    fun hoy(zona: ZoneId): LocalDate = reloj().atZone(zona).toLocalDate()

    /** Primer uso: el nombre de quien usa la app, para el saludo. */
    suspend fun guardarNombreUsuaria(nombre: String) {
        val limpio = Texto.limpiar(nombre) ?: throw ReglaDeNegocioException("Escribí tu nombre.")
        if (limpio.length > 150) throw ReglaDeNegocioException("El nombre puede tener hasta 150 caracteres.")
        db.parametros().guardar(ParametroEntidad(Parametros.CLAVE_NOMBRE, limpio))
    }

    /**
     * Cambia los ajustes. Ojo con la jornada: no recalcula ningún saldo (los
     * minutos guardados no se tocan), pero cambia cómo se MUESTRAN en días.
     *
     * @return true si cambió algo
     */
    suspend fun actualizarAjustes(horasPorJornada: Int, zonaHoraria: String, nombre: String): Boolean =
        db.withTransaction {
            if (horasPorJornada !in 1..24) throw ReglaDeNegocioException("Las horas por jornada van de 1 a 24.")
            val zona = runCatching { ZoneId.of(zonaHoraria.trim()) }.getOrNull()
                ?: throw ReglaDeNegocioException(
                    "No reconozco la zona horaria «$zonaHoraria». Para Nicaragua es America/Managua.",
                )
            val nombreLimpio = Texto.limpiar(nombre) ?: throw ReglaDeNegocioException("Escribí tu nombre.")
            val antes = parametrosActuales()
            val cambios = buildList {
                if (antes.horasPorJornada != horasPorJornada) {
                    add("Horas por jornada: ${antes.horasPorJornada} → $horasPorJornada")
                }
                if (antes.zona != zona) add("Zona horaria: ${antes.zona.id} → ${zona.id}")
                if (antes.nombreUsuaria != nombreLimpio) add("Nombre: ${antes.nombreUsuaria} → $nombreLimpio")
            }
            if (cambios.isEmpty()) return@withTransaction false

            db.parametros().guardar(ParametroEntidad(Parametros.CLAVE_JORNADA, horasPorJornada.toString()))
            db.parametros().guardar(ParametroEntidad(Parametros.CLAVE_ZONA, zona.id))
            db.parametros().guardar(ParametroEntidad(Parametros.CLAVE_NOMBRE, nombreLimpio))
            anotar(AccionBitacora.PARAMETRO_CAMBIADO, cambios.joinToString(" · "))
            true
        }

    // =========================================================================
    //  Funcionarios
    // =========================================================================

    /** Todos, ordenados como en una lista en papel: "Ángela" va con las A. */
    val funcionarios: Flow<List<FuncionarioEntidad>> =
        db.funcionarios().observarTodos().map { lista -> lista.sortedWith(compareBy(ordenEspanol) { it.nombreCompleto }) }

    fun funcionario(id: Long): Flow<FuncionarioEntidad?> = db.funcionarios().observar(id)

    /**
     * Filtra por nombre sin importar tildes ni mayúsculas: "angela perez"
     * encuentra a "Ángela Pérez". Con unas decenas de personas se hace en
     * memoria, igual que en la web.
     */
    fun buscar(lista: List<FuncionarioEntidad>, texto: String, incluirDadosDeBaja: Boolean): List<FuncionarioEntidad> {
        val buscado = Texto.normalizar(texto)
        return lista.filter { (incluirDadosDeBaja || it.activo) && Texto.normalizar(it.nombreCompleto).contains(buscado) }
    }

    /** Registra un funcionario con su saldo inicial. Todo o nada. */
    suspend fun crearFuncionario(borrador: BorradorFuncionario): Long = db.withTransaction {
        val p = parametrosActuales()
        val ahora = reloj()
        ReglasDeFuncionario.revisar(borrador, hoy(p.zona), esAlta = true).values.firstOrNull()?.let {
            throw ReglaDeNegocioException(it)
        }
        val minutos = ConversorTiempo.aMinutos(borrador.saldoDias, borrador.saldoHoras, 0, p.horasPorJornada)
        val nombre = Texto.limpiar(borrador.nombreCompleto)!!

        val id = db.funcionarios().insertar(
            FuncionarioEntidad(
                nombreCompleto = nombre,
                cargo = Texto.limpiar(borrador.cargo),
                areaODependencia = Texto.limpiar(borrador.areaODependencia),
                fechaIngreso = borrador.fechaIngreso,
                saldoMinutos = minutos,
                creadoEn = ahora,
                actualizadoEn = ahora,
            ),
        )
        // Si el saldo inicial es cero no hay movimiento: no hubo ningún cambio.
        if (minutos > 0) {
            db.movimientos().insertar(
                MovimientoEntidad(
                    funcionarioId = id, tipo = TipoMovimiento.SALDO_INICIAL, minutos = minutos,
                    saldoResultante = minutos, descripcion = "Saldo con el que se registró al funcionario",
                    creadoEn = ahora,
                ),
            )
        }
        anotar(
            AccionBitacora.FUNCIONARIO_REGISTRADO,
            "$nombre · Saldo inicial: ${ConversorTiempo.formatear(minutos, p.horasPorJornada)}",
        )
        id
    }

    /** Cambia los datos. El saldo NO se toca aunque venga en el borrador. */
    suspend fun editarFuncionario(id: Long, borrador: BorradorFuncionario) = db.withTransaction {
        val p = parametrosActuales()
        ReglasDeFuncionario.revisar(borrador, hoy(p.zona), esAlta = false).values.firstOrNull()?.let {
            throw ReglaDeNegocioException(it)
        }
        val actual = obtenerFuncionario(id)
        val nuevo = actual.copy(
            nombreCompleto = Texto.limpiar(borrador.nombreCompleto)!!,
            cargo = Texto.limpiar(borrador.cargo),
            areaODependencia = Texto.limpiar(borrador.areaODependencia),
            fechaIngreso = borrador.fechaIngreso,
            actualizadoEn = reloj(),
        )
        if (nuevo.copy(actualizadoEn = actual.actualizadoEn) != actual) {
            db.funcionarios().actualizar(nuevo)
            anotar(AccionBitacora.FUNCIONARIO_EDITADO, nuevo.nombreCompleto)
        }
    }

    /** Baja lógica: el funcionario queda guardado con todo su historial. */
    suspend fun darDeBaja(id: Long) = db.withTransaction {
        val f = obtenerFuncionario(id)
        if (!f.activo) throw ReglaDeNegocioException("${f.nombreCompleto} ya estaba de baja.")
        db.funcionarios().actualizar(f.copy(activo = false, actualizadoEn = reloj()))
        anotar(AccionBitacora.FUNCIONARIO_DADO_DE_BAJA, f.nombreCompleto)
    }

    /** Deshace una baja. El saldo y el historial siguen exactamente como estaban. */
    suspend fun reactivar(id: Long) = db.withTransaction {
        val f = obtenerFuncionario(id)
        if (f.activo) throw ReglaDeNegocioException("${f.nombreCompleto} no está de baja.")
        db.funcionarios().actualizar(f.copy(activo = true, actualizadoEn = reloj()))
        anotar(AccionBitacora.FUNCIONARIO_REACTIVADO, f.nombreCompleto)
    }

    private suspend fun obtenerFuncionario(id: Long): FuncionarioEntidad =
        db.funcionarios().obtener(id) ?: throw ReglaDeNegocioException("No encontramos a ese funcionario.")

    // =========================================================================
    //  Solicitudes
    // =========================================================================

    fun solicitudesDe(funcionarioId: Long): Flow<List<SolicitudEntidad>> =
        db.solicitudes().observarDeFuncionario(funcionarioId)

    fun movimientosDe(funcionarioId: Long): Flow<List<MovimientoEntidad>> =
        db.movimientos().observarDeFuncionario(funcionarioId)

    fun recientes(cantidad: Int = 5): Flow<List<SolicitudConFuncionario>> = db.solicitudes().observarRecientes(cantidad)

    /**
     * Cuántas solicitudes y cuánto tiempo hubo en el mes, por tipo. Cuenta las
     * que EMPIEZAN en el mes; las anuladas no se cuentan.
     */
    fun resumenDelMes(mes: YearMonth): Flow<Map<TipoSolicitud, ResumenTipo>> =
        db.solicitudes().observarEnPeriodo(mes.atDay(1), mes.atEndOfMonth()).map { filas ->
            TipoSolicitud.entries.associateWith { tipo ->
                val delTipo = filas.filter { it.solicitud.tipo == tipo && it.solicitud.estado == EstadoSolicitud.REGISTRADA }
                ResumenTipo(delTipo.size, delTipo.sumOf { it.solicitud.minutos })
            }
        }

    /** Revisa una solicitud sin guardar nada. Para la pantalla de confirmación. */
    suspend fun evaluar(borrador: BorradorSolicitud): EvaluacionSolicitud {
        val p = parametrosActuales()
        return evaluarCon(borrador, obtenerFuncionario(borrador.funcionarioId), p)
    }

    private suspend fun evaluarCon(borrador: BorradorSolicitud, f: FuncionarioEntidad, p: Parametros): EvaluacionSolicitud {
        val traslapes = if (borrador.fechaFin.isBefore(borrador.fechaInicio)) emptyList()
        else db.solicitudes().traslapes(f.id, borrador.fechaInicio, borrador.fechaFin)
            .map { SolicitudTraslapada(it.tipo, it.fechaInicio, it.fechaFin) }
        return EvaluadorSolicitud.evaluar(
            borrador, FuncionarioEvaluado(f.nombreCompleto, f.activo, f.saldoMinutos), traslapes,
            hoy(p.zona), p.horasPorJornada, p.nombreUsuaria,
        )
    }

    /**
     * Registra la solicitud y descuenta el saldo, todo o nada.
     *
     * Vuelve a evaluar TODO dentro de la transacción: entre la pantalla de
     * confirmación y este momento el saldo pudo haber cambiado.
     */
    suspend fun registrarSolicitud(
        borrador: BorradorSolicitud,
        autorizarNegativo: Boolean = false,
        motivoAutorizacion: String? = null,
    ): CambioDeSaldo = db.withTransaction {
        val p = parametrosActuales()
        val f = obtenerFuncionario(borrador.funcionarioId)
        val evaluacion = evaluarCon(borrador, f, p)
        val negativoAutorizado = EvaluadorSolicitud.autorizarGuardado(evaluacion, autorizarNegativo, motivoAutorizacion)
        val ahora = reloj()

        val solicitudId = db.solicitudes().insertar(
            SolicitudEntidad(
                funcionarioId = f.id, tipo = borrador.tipo, fechaInicio = borrador.fechaInicio,
                fechaFin = borrador.fechaFin, minutos = evaluacion.minutos, motivo = Texto.limpiar(borrador.motivo),
                creadoEn = ahora,
            ),
        )
        val descripcion = EvaluadorSolicitud.describir(borrador.tipo, borrador.fechaInicio, borrador.fechaFin)
        val motivoAut = Texto.limpiar(motivoAutorizacion)
        val cambio = aplicarMovimiento(
            f, TipoMovimiento.CONSUMO, -evaluacion.minutos, solicitudId,
            if (negativoAutorizado) "$descripcion · Saldo negativo autorizado: $motivoAut" else descripcion,
            negativoAutorizado,
        )

        val j = p.horasPorJornada
        anotar(
            AccionBitacora.SOLICITUD_REGISTRADA,
            "${f.nombreCompleto} · $descripcion · ${borrador.tipo.formatear(evaluacion.minutos, j)} · " +
                "Saldo: ${ConversorTiempo.formatear(cambio.saldoAntes, j)} → ${ConversorTiempo.formatear(cambio.saldoDespues, j)}",
        )
        if (negativoAutorizado) {
            anotar(
                AccionBitacora.SALDO_NEGATIVO_AUTORIZADO,
                "${f.nombreCompleto} quedó con un saldo de ${ConversorTiempo.formatear(cambio.saldoDespues, j)}. " +
                    "Motivo: $motivoAut",
            )
        }
        cambio
    }

    /** Anula una solicitud y devuelve su tiempo al saldo, todo o nada. No se borra nada. */
    suspend fun anularSolicitud(solicitudId: Long, motivo: String?): CambioDeSaldo = db.withTransaction {
        val s = db.solicitudes().obtener(solicitudId)
            ?: throw ReglaDeNegocioException("No encontramos esa solicitud.")
        if (s.estado == EstadoSolicitud.ANULADA) {
            throw ReglaDeNegocioException("Esta solicitud ya estaba anulada. El tiempo ya se había devuelto.")
        }
        val motivoLimpio = Texto.limpiar(motivo) ?: throw ReglaDeNegocioException("Escribí el motivo de la anulación.")
        if (motivoLimpio.length > ReglasDeSaldo.LARGO_DESCRIPCION) {
            throw ReglaDeNegocioException("El motivo puede tener hasta ${ReglasDeSaldo.LARGO_DESCRIPCION} caracteres.")
        }
        val ahora = reloj()
        db.solicitudes().actualizar(s.copy(estado = EstadoSolicitud.ANULADA, anuladoEn = ahora, motivoAnulacion = motivoLimpio))

        val f = obtenerFuncionario(s.funcionarioId)
        val descripcion = EvaluadorSolicitud.describir(s.tipo, s.fechaInicio, s.fechaFin)
        // Devolver tiempo siempre suma: nunca puede dejar un saldo negativo.
        val cambio = aplicarMovimiento(
            f, TipoMovimiento.REVERSION, s.minutos, s.id, "Anulación: $descripcion · $motivoLimpio", permitirNegativo = true,
        )
        val j = parametrosActuales().horasPorJornada
        anotar(
            AccionBitacora.SOLICITUD_ANULADA,
            "${f.nombreCompleto} · $descripcion · Se devolvieron ${s.tipo.formatear(s.minutos, j)} · Motivo: $motivoLimpio",
        )
        cambio
    }

    /** Ajuste manual: sumar o restar tiempo, siempre con motivo. */
    suspend fun ajustarSaldo(funcionarioId: Long, borrador: BorradorAjuste): CambioDeSaldo = db.withTransaction {
        val p = parametrosActuales()
        val f = obtenerFuncionario(funcionarioId)
        val revisado = ReglasDeAjuste.revisar(
            borrador, FuncionarioEvaluado(f.nombreCompleto, f.activo, f.saldoMinutos), p.horasPorJornada, p.nombreUsuaria,
        )
        val cambio = aplicarMovimiento(
            f, TipoMovimiento.AJUSTE_MANUAL, revisado.minutosConSigno, null, revisado.motivo, revisado.quedaNegativo,
        )
        val j = p.horasPorJornada
        val m = revisado.minutosConSigno
        anotar(
            AccionBitacora.SALDO_AJUSTADO,
            "${f.nombreCompleto} · ${if (m > 0) "Se sumaron" else "Se restaron"} " +
                "${ConversorTiempo.formatear(Math.abs(m), j)} · Saldo: ${ConversorTiempo.formatear(cambio.saldoAntes, j)}" +
                " → ${ConversorTiempo.formatear(cambio.saldoDespues, j)} · Motivo: ${revisado.motivo}",
        )
        if (revisado.quedaNegativo) {
            anotar(
                AccionBitacora.SALDO_NEGATIVO_AUTORIZADO,
                "${f.nombreCompleto} quedó con un saldo de ${ConversorTiempo.formatear(cambio.saldoDespues, j)} " +
                    "por un ajuste manual. Motivo: ${revisado.motivo}",
            )
        }
        cambio
    }

    /**
     * El ÚNICO lugar que cambia un saldo. Siempre se llama dentro de una
     * transacción, y escribe juntos el saldo nuevo y su movimiento.
     */
    private suspend fun aplicarMovimiento(
        f: FuncionarioEntidad,
        tipo: TipoMovimiento,
        minutos: Int,
        solicitudId: Long?,
        descripcion: String?,
        permitirNegativo: Boolean,
    ): CambioDeSaldo {
        check(db.inTransaction()) { "Un cambio de saldo tiene que hacerse dentro de una transacción." }
        val despues = ReglasDeSaldo.aplicar(f.saldoMinutos, minutos, permitirNegativo)
        val ahora = reloj()
        db.funcionarios().actualizar(f.copy(saldoMinutos = despues, actualizadoEn = ahora))
        db.movimientos().insertar(
            MovimientoEntidad(
                funcionarioId = f.id, solicitudId = solicitudId, tipo = tipo, minutos = minutos,
                saldoResultante = despues, descripcion = ReglasDeSaldo.recortar(descripcion), creadoEn = ahora,
            ),
        )
        return CambioDeSaldo(f.saldoMinutos, despues)
    }

    /** Cuánto se fue en el año en cada tipo. Solo cuenta las registradas, por su fecha de inicio. */
    fun desgloseDelAnio(solicitudes: List<SolicitudEntidad>, anio: Int): Map<TipoSolicitud, Int> =
        TipoSolicitud.entries.associateWith { tipo ->
            solicitudes.filter { it.tipo == tipo && it.estado == EstadoSolicitud.REGISTRADA && it.fechaInicio.year == anio }
                .sumOf { it.minutos }
        }

    // =========================================================================
    //  Bitácora, reportes y respaldos
    // =========================================================================

    val bitacora: Flow<List<BitacoraEntidad>> = db.bitacora().observarTodo()

    suspend fun anotar(accion: AccionBitacora, detalle: String?) {
        db.bitacora().insertar(BitacoraEntidad(accion = accion, detalle = ReglasDeSaldo.recortar(detalle), creadoEn = reloj()))
    }

    suspend fun datosParaReporteDeFuncionario(id: Long): Triple<FuncionarioEntidad, List<SolicitudEntidad>, List<MovimientoEntidad>> {
        val f = obtenerFuncionario(id)
        val solicitudes = db.solicitudes().todas().filter { it.funcionarioId == id }
            .sortedWith(compareByDescending<SolicitudEntidad> { it.fechaInicio }.thenByDescending { it.id })
        return Triple(f, solicitudes, db.movimientos().deFuncionario(id))
    }

    suspend fun solicitudesDelMes(mes: YearMonth): List<SolicitudConFuncionario> =
        db.solicitudes().enPeriodo(mes.atDay(1), mes.atEndOfMonth())

    /** Todo el contenido de la base, para el respaldo. */
    suspend fun contenidoCompleto(): ContenidoRespaldo = db.withTransaction {
        ContenidoRespaldo(
            parametros = db.parametros().todos().filter { it.clave != Parametros.CLAVE_ULTIMO_RESPALDO },
            funcionarios = db.funcionarios().todos(),
            solicitudes = db.solicitudes().todas(),
            movimientos = db.movimientos().todos(),
            bitacora = db.bitacora().todo(),
        )
    }

    suspend fun marcarRespaldoCreado(nombreArchivo: String) {
        db.parametros().guardar(ParametroEntidad(Parametros.CLAVE_ULTIMO_RESPALDO, reloj().toEpochMilli().toString()))
        anotar(AccionBitacora.RESPALDO_CREADO, nombreArchivo)
    }

    /**
     * Reemplaza TODOS los datos por los del respaldo. El contenido ya viene
     * validado por [Respaldo.leer]: si algo no cuadra, nunca llega acá.
     */
    suspend fun restaurar(contenido: ContenidoRespaldo, nombreArchivo: String) = db.withTransaction {
        val r = db.respaldo()
        r.vaciarMovimientos()
        r.vaciarSolicitudes()
        r.vaciarFuncionarios()
        r.vaciarBitacora()
        r.vaciarParametros()
        r.insertarParametros(contenido.parametros)
        r.insertarFuncionarios(contenido.funcionarios)
        r.insertarSolicitudes(contenido.solicitudes)
        r.insertarMovimientos(contenido.movimientos)
        r.insertarBitacora(contenido.bitacora)
        anotar(
            AccionBitacora.RESPALDO_RESTAURADO,
            "$nombreArchivo · ${contenido.funcionarios.size} funcionarios, ${contenido.solicitudes.size} solicitudes",
        )
    }
}
