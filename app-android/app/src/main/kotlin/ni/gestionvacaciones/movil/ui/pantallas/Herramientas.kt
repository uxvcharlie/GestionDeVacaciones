package ni.gestionvacaciones.movil.ui.pantallas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ni.gestionvacaciones.movil.datos.ContenidoRespaldo
import ni.gestionvacaciones.movil.datos.Parametros
import ni.gestionvacaciones.movil.datos.Respaldo
import ni.gestionvacaciones.movil.dominio.AccionBitacora
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.Fechas
import ni.gestionvacaciones.movil.dominio.PoliticaPin
import ni.gestionvacaciones.movil.dominio.ReglaDeNegocioException
import ni.gestionvacaciones.movil.reporte.ArchivoGenerado
import ni.gestionvacaciones.movil.reporte.FormatoReporte
import ni.gestionvacaciones.movil.reporte.Reportes
import ni.gestionvacaciones.movil.ui.LocalAvisar
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.Resultado
import ni.gestionvacaciones.movil.ui.Rutas
import ni.gestionvacaciones.movil.ui.componentes.Aviso
import ni.gestionvacaciones.movil.ui.componentes.BotonPeligro
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.componentes.BotonSecundario
import ni.gestionvacaciones.movil.ui.componentes.Campo
import ni.gestionvacaciones.movil.ui.componentes.CampoNumero
import ni.gestionvacaciones.movil.ui.componentes.DialogoConfirmacion
import ni.gestionvacaciones.movil.ui.componentes.PantallaConDesplazamiento
import ni.gestionvacaciones.movil.ui.componentes.Separador
import ni.gestionvacaciones.movil.ui.componentes.Subtitulo
import ni.gestionvacaciones.movil.ui.componentes.Tarjeta
import ni.gestionvacaciones.movil.ui.componentes.TextoSuave
import ni.gestionvacaciones.movil.ui.componentes.TipoAviso
import ni.gestionvacaciones.movil.ui.componentes.rememberExportador
import ni.gestionvacaciones.movil.ui.intentar
import ni.gestionvacaciones.movil.ui.tema.Paleta
import java.time.YearMonth

// =============================================================================
//  Reportes del mes
// =============================================================================

/** Reporte del mes en Excel o CSV. El de cada funcionario está en su ficha. */
@Composable
fun PantallaReportes(rutas: Rutas) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    var mes by rememberSaveable { mutableStateOf(YearMonth.from(repo.ahora().atZone(p.zona))) }
    val exportador = rememberExportador(
        "El reporte está listo",
        "Podés mandarlo por WhatsApp o correo, o guardarlo en el teléfono para imprimirlo después.",
    )

    PantallaConDesplazamiento("Reportes", alVolver = rutas::volver) {
        Subtitulo("Reporte del mes")
        TextoSuave("Todas las solicitudes que empiezan en el mes, también las anuladas (marcadas). En Excel trae además un resumen por tipo.")
        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotonSecundario("◀", { mes = mes.minusMonths(1) })
                Text(
                    Fechas.mes(mes),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = Paleta.Texto,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                BotonSecundario("▶", { mes = mes.plusMonths(1) })
            }
        }
        FormatoReporte.entries.forEach { formato ->
            BotonPrimario("Generar en ${formato.etiqueta}", {
                alcance.launch {
                    val r = intentar {
                        val filas = repo.solicitudesDelMes(mes)
                        Reportes.delMes(mes, filas, p.horasPorJornada, p.zona, repo.ahora(), formato).also {
                            repo.anotar(AccionBitacora.REPORTE_GENERADO, "Solicitudes de ${Fechas.mes(mes)} (${formato.etiqueta})")
                        }
                    }
                    when (r) {
                        is Resultado.Bien -> exportador.ofrecer(r.valor)
                        is Resultado.Mal -> avisar(r.mensaje)
                    }
                }
            }, Modifier.fillMaxWidth(), icono = "⬇")
        }
        Separador()
        Subtitulo("Reporte de un funcionario")
        TextoSuave("Está en la ficha de cada persona: abrí su ficha y tocá «Reporte Excel» o «Reporte CSV».")
        BotonSecundario("Ir a funcionarios", { rutas.funcionarios() }, Modifier.fillMaxWidth(), icono = "👥")
    }
}

// =============================================================================
//  Respaldo
// =============================================================================

/**
 * Crear y restaurar respaldos. Los datos viven solo en este celular: el
 * respaldo es lo que salva el trabajo si el teléfono se pierde.
 */
@Composable
fun PantallaRespaldo(rutas: Rutas) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())

    var leido by remember { mutableStateOf<Pair<ContenidoRespaldo, String>?>(null) }
    var errorRestaurar by remember { mutableStateOf<String?>(null) }

    val exportador = rememberExportador(
        "El respaldo está listo",
        "Guardalo fuera de este celular: mandalo a tu propio WhatsApp o correo, o guardalo en Google Drive. " +
            "Ojo: el archivo tiene los datos de los funcionarios, no lo mandes a nadie más.",
        alEntregar = { archivo -> alcance.launch { repo.marcarRespaldoCreado(archivo.nombre) } },
    )

    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            errorRestaurar = null
            val r = intentar {
                val texto = withContext(Dispatchers.IO) {
                    contexto.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } ?: throw ReglaDeNegocioException("No se pudo abrir el archivo.")
                Respaldo.leer(texto) to (uri.lastPathSegment?.substringAfterLast('/') ?: "respaldo")
            }
            when (r) {
                is Resultado.Bien -> leido = r.valor
                is Resultado.Mal -> errorRestaurar = r.mensaje
            }
        }
    }

    PantallaConDesplazamiento("Respaldo", alVolver = rutas::volver) {
        Aviso(
            "Los datos viven solo en este celular. Si el teléfono se pierde, se rompe o se cambia, " +
                "lo único que los salva es un respaldo guardado fuera de él. Hacé uno por semana.",
            TipoAviso.INFORMACION,
        )
        Tarjeta {
            Subtitulo("Crear un respaldo")
            TextoSuave(
                p.ultimoRespaldo?.let { "Último respaldo: ${Fechas.fechaHora(it, p.zona)}." }
                    ?: "Todavía no hiciste ningún respaldo.",
            )
            BotonPrimario("Crear respaldo ahora", {
                alcance.launch {
                    val r = intentar {
                        val contenido = repo.contenidoCompleto()
                        val ahora = repo.ahora()
                        val fecha = ahora.atZone(p.zona).toLocalDate()
                        ArchivoGenerado(
                            "respaldo-vacaciones-$fecha.json",
                            "application/json",
                            Respaldo.escribir(contenido, ahora).toByteArray(Charsets.UTF_8),
                        )
                    }
                    when (r) {
                        is Resultado.Bien -> exportador.ofrecer(r.valor)
                        is Resultado.Mal -> avisar(r.mensaje)
                    }
                }
            }, Modifier.fillMaxWidth(), icono = "💾")
        }

        Tarjeta {
            Subtitulo("Restaurar un respaldo")
            TextoSuave(
                "Para un celular nuevo o después de reinstalar la app. Reemplaza TODOS los datos de este celular " +
                    "por los del archivo. Antes de tocar nada se revisa que el archivo esté completo y sin alteraciones.",
            )
            errorRestaurar?.let { Aviso(it, TipoAviso.ERROR) }
            BotonPeligro("Elegir archivo de respaldo", { abrir.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }, Modifier.fillMaxWidth(), icono = "↺")
        }
    }

    leido?.let { (contenido, nombre) ->
        val creado = contenido.bitacora.maxOfOrNull { it.creadoEn }
        DialogoConfirmacion(
            titulo = "¿Reemplazar todos los datos?",
            texto = "El respaldo «$nombre» tiene ${contenido.funcionarios.size} funcionarios y ${contenido.solicitudes.size} solicitudes" +
                (creado?.let { ", con actividad hasta el ${Fechas.fechaHora(it, p.zona)}" } ?: "") +
                ". Todo lo que hay ahora en este celular se va a reemplazar por eso. No se puede deshacer.",
            confirmar = "Sí, restaurar",
            peligroso = true,
            alCancelar = { leido = null },
            alConfirmar = {
                leido = null
                alcance.launch {
                    when (val r = intentar { repo.restaurar(contenido, nombre) }) {
                        is Resultado.Bien -> {
                            avisar("Listo: se restauraron ${contenido.funcionarios.size} funcionarios y ${contenido.solicitudes.size} solicitudes.")
                            rutas.tablero()
                        }
                        is Resultado.Mal -> errorRestaurar = r.mensaje
                    }
                }
            },
        )
    }
}

// =============================================================================
//  Bitácora
// =============================================================================

@Composable
fun PantallaBitacora(rutas: Rutas) {
    val repo = LocalContenedor.current.repositorio
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    val entradas by repo.bitacora.collectAsStateWithLifecycle(null)
    var mostrar by rememberSaveable { mutableStateOf(50) }

    PantallaConDesplazamiento("Bitácora", alVolver = rutas::volver) {
        TextoSuave("Todo lo que se hizo en la app, de lo más nuevo a lo más viejo.")
        val lista = entradas ?: return@PantallaConDesplazamiento
        if (lista.isEmpty()) {
            TextoSuave("Todavía no hay nada registrado.")
            return@PantallaConDesplazamiento
        }
        Tarjeta {
            lista.take(mostrar).forEachIndexed { i, b ->
                if (i > 0) Separador()
                Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
                    Text(b.accion.etiqueta, color = Paleta.Texto, fontWeight = FontWeight.SemiBold)
                    TextoSuave(Fechas.fechaHora(b.creadoEn, p.zona))
                    b.detalle?.let { Text(it, color = Paleta.Suave, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        if (lista.size > mostrar) {
            BotonSecundario("Ver ${minOf(50, lista.size - mostrar)} más", { mostrar += 50 }, Modifier.fillMaxWidth())
        }
    }
}

// =============================================================================
//  Ajustes
// =============================================================================

@Composable
fun PantallaAjustes(rutas: Rutas) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    val p by repo.parametros.collectAsStateWithLifecycle(null)

    var cargado by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var jornada by rememberSaveable { mutableStateOf("") }
    var zona by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarJornada by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(p) {
        val actuales = p
        if (actuales != null && !cargado) {
            nombre = actuales.nombreUsuaria
            jornada = actuales.horasPorJornada.toString()
            zona = actuales.zona.id
            cargado = true
        }
    }

    fun guardar() {
        alcance.launch {
            when (val r = intentar { repo.actualizarAjustes(jornada.toIntOrNull() ?: 0, zona, nombre) }) {
                is Resultado.Bien -> {
                    avisar(if (r.valor) "Listo: se guardaron los ajustes." else "No había nada que cambiar.")
                    rutas.volver()
                }
                is Resultado.Mal -> error = r.mensaje
            }
        }
    }

    PantallaConDesplazamiento("Ajustes", alVolver = rutas::volver) {
        val actuales = p ?: return@PantallaConDesplazamiento
        if (!cargado) return@PantallaConDesplazamiento
        error?.let { Aviso(it, TipoAviso.ERROR) }

        Campo("Tu nombre", nombre, { nombre = it }, ayuda = "Se usa para saludarte.", mayusculas = KeyboardCapitalization.Words)
        CampoNumero("Horas por jornada", jornada, { jornada = it }, Modifier.fillMaxWidth(), digitos = 2)
        TextoSuave("Cuántas horas tiene un día de trabajo completo. Se usa para mostrar los saldos en días.")
        Campo("Zona horaria", zona, { zona = it }, ayuda = "Para Nicaragua: America/Managua.", mayusculas = KeyboardCapitalization.None)

        BotonPrimario("Guardar ajustes", {
            error = null
            val nueva = jornada.toIntOrNull()
            if (nueva != null && nueva != actuales.horasPorJornada) confirmarJornada = true else guardar()
        }, Modifier.fillMaxWidth(), icono = "✓")

        Separador()
        Subtitulo("Seguridad")
        BotonSecundario("Cambiar el PIN", { rutas.cambiarPin() }, Modifier.fillMaxWidth(), icono = "🔒")
        TextoSuave("La app se bloquea sola después de 30 minutos sin usarla, y cada vez que se vuelve a abrir.")
    }

    if (confirmarJornada) {
        val nueva = jornada.toIntOrNull() ?: 0
        val ejemplo = 10 * (p?.horasPorJornada ?: 8) * 60
        DialogoConfirmacion(
            titulo = "¿Cambiar la jornada a $nueva horas?",
            texto = "Ningún saldo se recalcula: los minutos guardados no se tocan. Pero cambia cómo se MUESTRAN en días. " +
                "Por ejemplo, alguien que hoy tiene ${ConversorTiempo.formatear(ejemplo, p?.horasPorJornada ?: 8)} " +
                "se va a ver con ${if (nueva in 1..24) ConversorTiempo.formatear(ejemplo, nueva) else "?"}.",
            confirmar = "Sí, cambiar",
            alCancelar = { confirmarJornada = false },
            alConfirmar = { confirmarJornada = false; guardar() },
        )
    }
}

/** Cambiar el PIN: el actual, el nuevo y la confirmación. */
@Composable
fun PantallaCambiarPin(rutas: Rutas) {
    val contenedor = LocalContenedor.current
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    var paso by rememberSaveable { mutableStateOf(1) }
    var nuevo by rememberSaveable { mutableStateOf("") }

    when (paso) {
        1 -> TecladoPin(
            titulo = "Tu PIN actual",
            explicacion = "Para cambiarlo, primero escribí el que usás hoy.",
            alVolver = rutas::volver,
            alCompletar = { pin, mostrarError ->
                alcance.launch {
                    when (val r = withContext(Dispatchers.Default) { contenedor.acceso.verificar(pin) }) {
                        ni.gestionvacaciones.movil.acceso.ResultadoPin.Correcto -> paso = 2
                        is ni.gestionvacaciones.movil.acceso.ResultadoPin.Incorrecto ->
                            mostrarError("PIN equivocado. Te quedan ${r.intentosQueQuedan} intentos.")
                        else -> contenedor.sesion.bloquear() // se bloqueó: vuelve a la pantalla del PIN
                    }
                }
            },
        )
        2 -> TecladoPin(
            titulo = "Tu PIN nuevo",
            explicacion = PoliticaPin.EXPLICACION,
            alVolver = rutas::volver,
            alCompletar = { pin, mostrarError ->
                val problema = PoliticaPin.revisar(pin)
                if (problema != null) mostrarError(problema) else { nuevo = pin; paso = 3 }
            },
        )
        else -> TecladoPin(
            titulo = "Repetí el PIN nuevo",
            explicacion = "Escribilo otra vez para confirmar.",
            alVolver = { nuevo = ""; paso = 2 },
            alCompletar = { pin, mostrarError ->
                if (pin != nuevo) {
                    mostrarError("No coinciden. Volvé a escribir el PIN nuevo.")
                    nuevo = ""
                    paso = 2
                } else {
                    alcance.launch {
                        withContext(Dispatchers.Default) { contenedor.acceso.crearPin(pin) }
                        contenedor.repositorio.anotar(AccionBitacora.PIN_CAMBIADO, null)
                        avisar("Listo: tu PIN cambió.")
                        rutas.volver()
                    }
                }
            },
        )
    }
}
