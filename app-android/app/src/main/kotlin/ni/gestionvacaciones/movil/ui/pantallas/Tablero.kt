package ni.gestionvacaciones.movil.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ni.gestionvacaciones.movil.datos.FuncionarioEntidad
import ni.gestionvacaciones.movil.datos.Parametros
import ni.gestionvacaciones.movil.datos.ResumenTipo
import ni.gestionvacaciones.movil.datos.SolicitudConFuncionario
import ni.gestionvacaciones.movil.dominio.ConversorTiempo
import ni.gestionvacaciones.movil.dominio.EstadoSolicitud
import ni.gestionvacaciones.movil.dominio.Fechas
import ni.gestionvacaciones.movil.dominio.Saludo
import ni.gestionvacaciones.movil.dominio.Texto
import ni.gestionvacaciones.movil.dominio.TipoSolicitud
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.Rutas
import ni.gestionvacaciones.movil.ui.componentes.ALTO_MINIMO
import ni.gestionvacaciones.movil.ui.componentes.Aviso
import ni.gestionvacaciones.movil.ui.componentes.BotonBarra
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.componentes.BotonSecundario
import ni.gestionvacaciones.movil.ui.componentes.BotonTexto
import ni.gestionvacaciones.movil.ui.componentes.Campo
import ni.gestionvacaciones.movil.ui.componentes.Encabezado
import ni.gestionvacaciones.movil.ui.componentes.EstadoVacio
import ni.gestionvacaciones.movil.ui.componentes.Insignia
import ni.gestionvacaciones.movil.ui.componentes.Separador
import ni.gestionvacaciones.movil.ui.componentes.Subtitulo
import ni.gestionvacaciones.movil.ui.componentes.Tarjeta
import ni.gestionvacaciones.movil.ui.componentes.TextoSuave
import ni.gestionvacaciones.movil.ui.componentes.TipoAviso
import ni.gestionvacaciones.movil.ui.tema.Paleta
import java.time.Duration
import java.time.YearMonth

/**
 * La pantalla principal: el saludo, el botón grande para registrar, el
 * buscador, el resumen del mes y las últimas solicitudes.
 */
@Composable
fun PantallaTablero(rutas: Rutas) {
    val repo = LocalContenedor.current.repositorio
    val sesion = LocalContenedor.current.sesion
    val p by repo.parametros.collectAsStateWithLifecycle(Parametros())
    val funcionarios by repo.funcionarios.collectAsStateWithLifecycle(null)
    val recientes by remember { repo.recientes(5) }.collectAsStateWithLifecycle(emptyList())

    // La hora se vuelve a leer cada minuto: el saludo cambia solo a las 12:00.
    var ahora by remember { mutableStateOf(repo.ahora()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            ahora = repo.ahora()
        }
    }
    val local = ahora.atZone(p.zona)
    val mes = YearMonth.from(local)
    val resumen by remember(mes) { repo.resumenDelMes(mes) }.collectAsStateWithLifecycle(emptyMap())

    Column(Modifier.fillMaxSize()) {
        Encabezado("Registro de Vacaciones", alVolver = null) {
            BotonBarra("Bloquear", { sesion.bloquear() })
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text(
                        Saludo.para(local.toLocalTime()) + (Texto.primerNombre(p.nombreUsuaria).takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Paleta.Texto,
                        modifier = Modifier.semantics { heading() },
                    )
                    TextoSuave("Hoy es " + Fechas.fechaLarga(local.toLocalDate()))
                }

                val lista = funcionarios
                val hayFuncionarios = !lista.isNullOrEmpty()

                if (hayFuncionarios) {
                    RecordatorioRespaldo(p, ahora, rutas)
                }

                BotonPrimario(
                    "Registrar solicitud",
                    { rutas.solicitud() },
                    Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    icono = "＋",
                )

                if (lista != null && lista.isEmpty()) {
                    EstadoVacio(
                        "Todavía no hay funcionarios registrados",
                        "Empezá agregando a la primera persona con su saldo. Después vas a poder registrarle solicitudes.",
                        "Agregar el primero",
                    ) { rutas.nuevoFuncionario() }
                }

                if (lista != null && hayFuncionarios) {
                    Buscador(lista, p.horasPorJornada, rutas)
                    ResumenDelMes(mes, lista.count { it.activo }, resumen, p.horasPorJornada, rutas)
                    UltimasSolicitudes(recientes, p.horasPorJornada, rutas)
                }

                Separador()
                Subtitulo("Más opciones")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonSecundario("Funcionarios", { rutas.funcionarios() }, Modifier.fillMaxWidth(), icono = "👥")
                    BotonSecundario("Reportes en Excel o CSV", { rutas.reportes() }, Modifier.fillMaxWidth(), icono = "📊")
                    BotonSecundario("Respaldo de los datos", { rutas.respaldo() }, Modifier.fillMaxWidth(), icono = "💾")
                    BotonSecundario("Bitácora", { rutas.bitacora() }, Modifier.fillMaxWidth(), icono = "🗒")
                    BotonSecundario("Ajustes", { rutas.ajustes() }, Modifier.fillMaxWidth(), icono = "⚙")
                }
                Spacer(Modifier.heightIn(min = 16.dp))
            }
        }
    }
}

/**
 * Los datos viven solo en este celular: si pasa más de una semana sin
 * respaldo, se le recuerda a Brenda. No bloquea nada.
 */
@Composable
private fun RecordatorioRespaldo(p: Parametros, ahora: java.time.Instant, rutas: Rutas) {
    val ultimo = p.ultimoRespaldo
    val dias = ultimo?.let { Duration.between(it, ahora).toDays() }
    if (ultimo != null && dias != null && dias < 7) return
    Aviso(
        if (ultimo == null) "Todavía no hiciste ningún respaldo. Si este celular se pierde, se pierden los datos."
        else "Tu último respaldo fue hace $dias días. Conviene hacer uno por semana.",
        TipoAviso.ADVERTENCIA,
    )
    BotonSecundario("Hacer un respaldo ahora", { rutas.respaldo() }, Modifier.fillMaxWidth(), icono = "💾")
}

@Composable
private fun Buscador(lista: List<FuncionarioEntidad>, jornada: Int, rutas: Rutas) {
    val repo = LocalContenedor.current.repositorio
    var texto by rememberSaveable { mutableStateOf("") }
    Tarjeta {
        Subtitulo("Buscar funcionario")
        Campo(
            etiqueta = "Nombre del funcionario",
            valor = texto,
            alCambiar = { texto = it },
            ayuda = "Escribí parte del nombre. No importan las tildes.",
        )
        if (texto.isNotBlank()) {
            val encontrados = repo.buscar(lista, texto, incluirDadosDeBaja = true)
            if (encontrados.isEmpty()) {
                TextoSuave("No hay nadie con «${texto.trim()}». Revisá cómo está escrito.")
            } else {
                encontrados.take(8).forEach { FilaFuncionario(it, jornada) { rutas.ficha(it.id) } }
                if (encontrados.size > 8) TextoSuave("Y ${encontrados.size - 8} más. Escribí un poco más del nombre.")
            }
        }
    }
}

/** Un funcionario en una lista: nombre, cargo, saldo. Se toca para abrir la ficha. */
@Composable
fun FilaFuncionario(f: FuncionarioEntidad, jornada: Int, alTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ALTO_MINIMO)
            .clickable(onClickLabel = "Abrir la ficha de ${f.nombreCompleto}", role = Role.Button, onClick = alTocar)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(f.nombreCompleto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Paleta.Texto)
            val detalle = listOfNotNull(f.cargo, f.areaODependencia).joinToString(" · ")
            if (detalle.isNotEmpty()) TextoSuave(detalle)
            if (!f.activo) Insignia("De baja", "⊘", Paleta.Alerta)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                ConversorTiempo.formatear(f.saldoMinutos, jornada),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (f.saldoMinutos < 0) Paleta.Error else Paleta.AcentoTexto,
            )
            TextoSuave("de saldo")
        }
    }
}

@Composable
private fun ResumenDelMes(mes: YearMonth, activos: Int, resumen: Map<TipoSolicitud, ResumenTipo>, jornada: Int, rutas: Rutas) {
    Subtitulo("Resumen de ${Fechas.mes(mes)}")
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TextoSuave("Funcionarios activos")
                Text("$activos", style = MaterialTheme.typography.headlineMedium, color = Paleta.Texto)
            }
            BotonTexto("Ver todos", { rutas.funcionarios() })
        }
    }
    TipoSolicitud.entries.forEach { tipo ->
        val r = resumen[tipo] ?: ResumenTipo()
        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tipo.icono, modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f)) {
                    Text(tipo.etiqueta, style = MaterialTheme.typography.titleMedium, color = Paleta.Texto)
                    TextoSuave(
                        (if (r.cantidad == 1) "1 solicitud" else "${r.cantidad} solicitudes") +
                            " · " + tipo.formatear(r.minutos, jornada),
                    )
                }
                Text("${r.cantidad}", style = MaterialTheme.typography.headlineMedium, color = Paleta.AcentoTexto)
            }
        }
    }
    TextoSuave("Cuenta las solicitudes que empiezan este mes. Las anuladas no se cuentan.")
}

@Composable
private fun UltimasSolicitudes(recientes: List<SolicitudConFuncionario>, jornada: Int, rutas: Rutas) {
    Subtitulo("Últimas solicitudes")
    if (recientes.isEmpty()) {
        TextoSuave("Todavía no se registró ninguna solicitud. Cuando registres la primera, aparece acá.")
        return
    }
    Tarjeta {
        recientes.forEachIndexed { i, r ->
            if (i > 0) Separador()
            val s = r.solicitud
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ALTO_MINIMO)
                    .clickable(onClickLabel = "Abrir la ficha de ${r.nombreFuncionario}", role = Role.Button) {
                        rutas.ficha(s.funcionarioId)
                    }
                    .padding(vertical = 6.dp),
            ) {
                Text(r.nombreFuncionario, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Paleta.Texto)
                TextoSuave("${s.tipo.etiqueta} ${Fechas.rango(s.fechaInicio, s.fechaFin)} · ${s.tipo.formatear(s.minutos, jornada)}")
                if (s.estado == EstadoSolicitud.ANULADA) Insignia("Anulada", "✕", Paleta.Error)
            }
        }
    }
}
