package ni.gestionvacaciones.movil.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ni.gestionvacaciones.movil.datos.FuncionarioEntidad
import ni.gestionvacaciones.movil.datos.Parametros
import ni.gestionvacaciones.movil.dominio.BorradorSolicitud
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.EvaluacionSolicitud
import ni.gestionvacaciones.movil.dominio.EvaluadorSolicitud
import ni.gestionvacaciones.movil.dominio.Fechas
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import ni.gestionvacaciones.movil.ui.LocalAvisar
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.Resultado
import ni.gestionvacaciones.movil.ui.Rutas
import ni.gestionvacaciones.movil.ui.componentes.ALTO_MINIMO
import ni.gestionvacaciones.movil.ui.componentes.Aviso
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.componentes.BotonSecundario
import ni.gestionvacaciones.movil.ui.componentes.BotonTexto
import ni.gestionvacaciones.movil.ui.componentes.Campo
import ni.gestionvacaciones.movil.ui.componentes.CampoNumero
import ni.gestionvacaciones.movil.ui.componentes.Encabezado
import ni.gestionvacaciones.movil.ui.componentes.EstadoVacio
import ni.gestionvacaciones.movil.ui.componentes.PantallaConDesplazamiento
import ni.gestionvacaciones.movil.ui.componentes.SelectorFecha
import ni.gestionvacaciones.movil.ui.componentes.Separador
import ni.gestionvacaciones.movil.ui.componentes.Subtitulo
import ni.gestionvacaciones.movil.ui.componentes.Tarjeta
import ni.gestionvacaciones.movil.ui.componentes.TextoSuave
import ni.gestionvacaciones.movil.ui.componentes.TipoAviso
import ni.gestionvacaciones.movil.ui.intentar
import ni.gestionvacaciones.movil.ui.tema.Paleta
import java.time.LocalDate

private const val NOTA_MEDICA =
    "Recordá: en una cita médica no se escribe ningún dato de salud (ni diagnósticos ni detalles). " +
        "Basta con dejar el motivo vacío o poner algo como «Cita en el hospital»."

/**
 * Registrar una solicitud en dos pasos:
 *   1. Funcionario, tipo, fechas y cantidad.
 *   2. Resumen antes de guardar: "María González — Vacaciones — del 10/11/2026
 *      al 14/11/2026 — 5 días. Saldo: 12 días → 7 días".
 * Nada se guarda hasta tocar «Guardar solicitud».
 */
@Composable
fun PantallaSolicitud(rutas: Rutas, funcionarioInicial: Long?) {
    val repo = LocalContenedor.current.repositorio
    val avisar = LocalAvisar.current
    val alcance = rememberCoroutineScope()
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    val funcionarios by repo.funcionarios.collectAsStateWithLifecycle(null)

    val hoy = remember(p.zona) { repo.hoy(p.zona) }
    var paso by rememberSaveable { mutableStateOf(1) }
    var funcionarioId by rememberSaveable { mutableStateOf(funcionarioInicial) }
    var tipo by rememberSaveable { mutableStateOf<TipoSolicitud?>(null) }
    var inicio by rememberSaveable { mutableStateOf(hoy) }
    var fin by rememberSaveable { mutableStateOf(hoy) }
    var dias by rememberSaveable { mutableStateOf("") }
    var horasSueltas by rememberSaveable { mutableStateOf("") }
    var horas by rememberSaveable { mutableStateOf("") }
    var minutos by rememberSaveable { mutableStateOf("") }
    var motivo by rememberSaveable { mutableStateOf("") }
    var autorizar by rememberSaveable { mutableStateOf(false) }
    var motivoAutorizacion by rememberSaveable { mutableStateOf("") }

    var eligiendo by rememberSaveable { mutableStateOf(false) }
    var faltantes by remember { mutableStateOf<List<String>>(emptyList()) }
    var evaluacion by remember { mutableStateOf<EvaluacionSolicitud?>(null) }
    var errorGuardado by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val lista = funcionarios
    val elegido = lista?.firstOrNull { it.id == funcionarioId }

    fun borrador() = BorradorSolicitud(
        funcionarioId = funcionarioId ?: 0,
        tipo = tipo ?: TipoSolicitud.VACACIONES,
        fechaInicio = inicio,
        fechaFin = fin,
        dias = dias.toIntOrNull() ?: 0,
        horasSueltas = horasSueltas.toIntOrNull() ?: 0,
        horas = horas.toIntOrNull() ?: 0,
        minutos = minutos.toIntOrNull() ?: 0,
        motivo = motivo,
    )

    // Si la app se cerró en el paso 2, se vuelve a calcular el resumen.
    LaunchedEffect(paso) {
        if (paso == 2 && evaluacion == null) {
            when (val r = intentar { repo.evaluar(borrador()) }) {
                is Resultado.Bien -> evaluacion = r.valor
                is Resultado.Mal -> { errorGuardado = r.mensaje; paso = 1 }
            }
        }
    }
    BackHandler(enabled = paso == 2) { paso = 1; evaluacion = null }

    if (paso == 1) {
        PantallaConDesplazamiento("Registrar solicitud", alVolver = rutas::volver) {
            TextoSuave("Paso 1 de 2. Completá los datos; en el paso siguiente revisás el resumen antes de guardar.")
            errorGuardado?.let { Aviso(it, TipoAviso.ERROR) }
            if (faltantes.isNotEmpty()) Aviso(faltantes.joinToString("\n"), TipoAviso.ERROR)

            if (lista != null && lista.none { it.activo }) {
                EstadoVacio(
                    "Todavía no hay funcionarios activos",
                    "Para registrar una solicitud primero tiene que haber al menos un funcionario.",
                    "Agregar funcionario",
                ) { rutas.nuevoFuncionario() }
                return@PantallaConDesplazamiento
            }

            // --- Funcionario ---
            Subtitulo("Funcionario")
            if (elegido == null) {
                BotonSecundario("Elegir funcionario", { eligiendo = true }, Modifier.fillMaxWidth(), icono = "👤")
            } else {
                Tarjeta {
                    Text(elegido.nombreCompleto, style = MaterialTheme.typography.titleMedium, color = Paleta.Texto)
                    listOfNotNull(elegido.cargo, elegido.areaODependencia).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { TextoSuave(it) }
                    Text("Saldo: " + ConversorTiempo.formatear(elegido.saldoMinutos, p.horasPorJornada), color = Paleta.AcentoTexto)
                    BotonTexto("Cambiar de funcionario", { eligiendo = true })
                }
            }

            // --- Tipo ---
            Subtitulo("Tipo")
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoSolicitud.entries.forEach { t ->
                    OpcionTipo(t, seleccionado = tipo == t) { tipo = t; faltantes = emptyList() }
                }
            }

            // --- Fechas ---
            Subtitulo("Fechas")
            SelectorFecha("Desde", inicio, { nueva ->
                if (nueva != null) {
                    // Comodidad: si era de un solo día, o si "hasta" quedaría
                    // antes de "desde", "hasta" se corre junto. Así un permiso
                    // de un día se resuelve eligiendo UNA sola fecha.
                    if (fin == inicio || fin.isBefore(nueva)) fin = nueva
                    inicio = nueva
                }
            })
            SelectorFecha("Hasta", fin, { nueva -> if (nueva != null) fin = nueva })
            if (fin.isBefore(inicio)) {
                Aviso("La fecha final no puede ser anterior a la fecha de inicio.", TipoAviso.ERROR)
            } else {
                val corridos = Fechas.diasCorridos(inicio, fin)
                TextoSuave(
                    if (corridos == 1L) "Es un solo día. Si son varios, cambiá la fecha «Hasta»."
                    else "Ese rango abarca $corridos días corridos (contando sábados y domingos).",
                )
            }

            // --- Cantidad, según el tipo ---
            when (tipo) {
                TipoSolicitud.VACACIONES -> {
                    Subtitulo("¿Cuántos días de vacaciones?")
                    TextoSuave("Para medio día usá las horas sueltas: con jornada de ${p.horasPorJornada} horas, medio día son ${p.horasPorJornada / 2}.")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoNumero("Días", dias, { dias = it }, Modifier.weight(1f), digitos = 3)
                        CampoNumero("Horas sueltas", horasSueltas, { horasSueltas = it }, Modifier.weight(1f), digitos = 2)
                    }
                }
                TipoSolicitud.CITA_MEDICA, TipoSolicitud.PERMISO -> {
                    Subtitulo("¿Cuánto tiempo?")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoNumero("Horas", horas, { horas = it }, Modifier.weight(1f), digitos = 2)
                        CampoNumero("Minutos", minutos, { minutos = it }, Modifier.weight(1f), digitos = 2)
                    }
                }
                null -> TextoSuave("Elegí el tipo para escribir la cantidad.")
            }

            // --- Motivo ---
            Campo("Motivo (opcional)", motivo, { motivo = it }, lineas = 2, largoMaximo = 300)
            if (tipo == TipoSolicitud.CITA_MEDICA) Aviso(NOTA_MEDICA, TipoAviso.INFORMACION)

            BotonPrimario("Revisar antes de guardar", {
                errorGuardado = null
                faltantes = buildList {
                    if (elegido == null) add("Elegí a qué funcionario le vas a registrar la solicitud.")
                    if (tipo == null) add("Elegí el tipo: vacaciones, cita médica o permiso.")
                }
                if (faltantes.isNotEmpty()) return@BotonPrimario
                alcance.launch {
                    when (val r = intentar { repo.evaluar(borrador()) }) {
                        is Resultado.Bien -> {
                            evaluacion = r.valor
                            autorizar = false
                            motivoAutorizacion = ""
                            paso = 2
                        }
                        is Resultado.Mal -> errorGuardado = r.mensaje
                    }
                }
            }, Modifier.fillMaxWidth(), icono = "→")
        }
    } else {
        PantallaConDesplazamiento("Revisá antes de guardar", alVolver = { paso = 1; evaluacion = null }) {
            val e = evaluacion
            if (e == null) {
                TextoSuave("Calculando…")
                return@PantallaConDesplazamiento
            }
            val j = p.horasPorJornada
            TextoSuave("Paso 2 de 2. Todavía no se guardó nada.")

            Tarjeta {
                Subtitulo("Resumen de la solicitud")
                Text(
                    "${e.funcionario.nombreCompleto} — ${e.tipo.etiqueta} — ${Fechas.rango(e.fechaInicio, e.fechaFin)} — " +
                        e.tipo.formatear(e.minutos, j),
                    style = MaterialTheme.typography.titleMedium,
                    color = Paleta.Texto,
                )
                if (e.diasCorridos > 1) TextoSuave("Ese rango abarca ${e.diasCorridos} días corridos.")
                Separador()
                Text(
                    "Saldo: ${ConversorTiempo.formatear(e.saldoAntes, j)} → queda en ${ConversorTiempo.formatear(e.saldoDespues, j)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (e.saldoDespues < 0) Paleta.Error else Paleta.AcentoTexto,
                )
                motivo.trim().takeIf { it.isNotEmpty() }?.let { TextoSuave("Motivo: $it") }
            }
            if (e.tipo == TipoSolicitud.CITA_MEDICA && motivo.isNotBlank()) {
                Aviso("$NOTA_MEDICA Si el motivo tiene alguno, tocá «Corregir datos».", TipoAviso.INFORMACION)
            }

            if (e.tieneErrores) {
                e.errores.forEach { Aviso(it, TipoAviso.ERROR) }
                TextoSuave("Así no se puede guardar. Corregí los datos y volvé a revisar.")
            }
            if (e.advertencias.isNotEmpty()) {
                Aviso(
                    "Revisá esto antes de guardar. No impide guardar.\n\n" + e.advertencias.joinToString("\n\n"),
                    TipoAviso.ADVERTENCIA,
                )
            }
            if (e.requiereAutorizacion) {
                Tarjeta {
                    Aviso(e.mensajeSaldoInsuficiente!!, TipoAviso.ERROR)
                    TextoSuave(
                        "Así no se puede guardar. Si esta excepción está autorizada, marcala y escribí el motivo: " +
                            "queda registrada en la bitácora con la fecha y la hora.",
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = ALTO_MINIMO)
                            .toggleable(value = autorizar, role = Role.Checkbox, onValueChange = { autorizar = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(autorizar, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Paleta.Primario, uncheckedColor = Paleta.Suave))
                        Text(
                            "Autorizo registrar esta solicitud aunque el saldo quede en negativo.",
                            Modifier.weight(1f).padding(start = 8.dp),
                            color = Paleta.Texto,
                        )
                    }
                    if (autorizar) {
                        Campo(
                            "Motivo de la autorización", motivoAutorizacion, { motivoAutorizacion = it },
                            lineas = 2, largoMaximo = EvaluadorSolicitud.LARGO_MOTIVO_AUTORIZACION,
                        )
                    }
                }
            }

            errorGuardado?.let { Aviso(it, TipoAviso.ERROR) }

            if (!e.tieneErrores) {
                BotonPrimario("Guardar solicitud", {
                    if (guardando) return@BotonPrimario
                    guardando = true
                    errorGuardado = null
                    alcance.launch {
                        val r = intentar { repo.registrarSolicitud(borrador(), autorizar, motivoAutorizacion) }
                        guardando = false
                        when (r) {
                            is Resultado.Bien -> {
                                avisar(
                                    "Listo: se registró. Saldo de ${e.funcionario.nombreCompleto}: " +
                                        "${ConversorTiempo.formatear(r.valor.saldoAntes, j)} → ${ConversorTiempo.formatear(r.valor.saldoDespues, j)}.",
                                )
                                if (funcionarioInicial != null) rutas.volver() else rutas.fichaTrasSolicitud(funcionarioId!!)
                            }
                            is Resultado.Mal -> errorGuardado = r.mensaje
                        }
                    }
                }, Modifier.fillMaxWidth(), icono = "✓", habilitado = !guardando)
            }
            BotonSecundario("Corregir datos", { paso = 1; evaluacion = null }, Modifier.fillMaxWidth(), icono = "✎")
        }
    }

    if (eligiendo && lista != null) {
        SelectorFuncionario(
            lista = lista.filter { it.activo },
            jornada = p.horasPorJornada,
            alElegir = { funcionarioId = it.id; eligiendo = false; faltantes = emptyList() },
            alCerrar = { eligiendo = false },
        )
    }
}

@Composable
private fun OpcionTipo(tipo: TipoSolicitud, seleccionado: Boolean, alElegir: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) Paleta.Elevada else Paleta.Superficie,
        // Igual que la web: la opción elegida lleva borde de foco y fondo; el
        // círculo del radio también cambia, así no depende solo del color.
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) Paleta.Foco else Paleta.Suave),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = alElegir),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = seleccionado, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Paleta.Primario, unselectedColor = Paleta.Suave))
            Text(tipo.icono, Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.titleLarge)
            Column {
                Text(tipo.etiqueta, style = MaterialTheme.typography.titleMedium, color = Paleta.Texto)
                TextoSuave(if (tipo.seCuentaEnDias) "Se escribe en días" else "Se escribe en horas y minutos")
            }
        }
    }
}

/** Ventana para elegir funcionario, con buscador. Muestra cargo y área para distinguir homónimos. */
@Composable
private fun SelectorFuncionario(
    lista: List<FuncionarioEntidad>,
    jornada: Int,
    alElegir: (FuncionarioEntidad) -> Unit,
    alCerrar: () -> Unit,
) {
    val repo = LocalContenedor.current.repositorio
    var texto by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Paleta.Fondo) {
            Column {
                Encabezado("Elegí un funcionario", alVolver = alCerrar)
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Campo("Buscar por nombre", texto, { texto = it }, ayuda = "Si hay dos personas con el mismo nombre, fijate en el cargo.")
                    val visibles = repo.buscar(lista, texto, incluirDadosDeBaja = false)
                    if (visibles.isEmpty()) TextoSuave("No hay nadie activo que coincida.")
                    Box {
                        Tarjeta {
                            visibles.forEachIndexed { i, f ->
                                if (i > 0) Separador()
                                FilaFuncionario(f, jornada) { alElegir(f) }
                            }
                        }
                    }
                }
            }
        }
    }
}
