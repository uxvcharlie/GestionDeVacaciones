package ni.gestionvacaciones.movil.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ni.gestionvacaciones.movil.acceso.ResultadoPin
import ni.gestionvacaciones.movil.dominio.AccionBitacora
import ni.gestionvacaciones.movil.dominio.PoliticaPin
import ni.gestionvacaciones.movil.ui.LocalContenedor
import ni.gestionvacaciones.movil.ui.Resultado
import ni.gestionvacaciones.movil.ui.componentes.Aviso
import ni.gestionvacaciones.movil.ui.componentes.BotonPrimario
import ni.gestionvacaciones.movil.ui.componentes.BotonTexto
import ni.gestionvacaciones.movil.ui.componentes.Campo
import ni.gestionvacaciones.movil.ui.componentes.TextoSuave
import ni.gestionvacaciones.movil.ui.componentes.TipoAviso
import ni.gestionvacaciones.movil.ui.componentes.Titulo
import ni.gestionvacaciones.movil.ui.intentar
import ni.gestionvacaciones.movil.ui.tema.Paleta

/** Columna centrada para las pantallas de acceso (sin encabezado). */
@Composable
private fun PantallaCentrada(contenido: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("◆", color = Paleta.Acento, fontSize = 40.sp, modifier = Modifier.clearAndSetSemantics { })
            contenido()
        }
    }
}

/**
 * El primer uso: cómo se llama quien usa la app (para el saludo) y su PIN.
 * El nombre sale de la base de datos, no está escrito en el código.
 */
@Composable
fun PantallaPrimerUso(alTerminar: () -> Unit) {
    val contenedor = LocalContenedor.current
    val alcance = rememberCoroutineScope()
    var paso by rememberSaveable { mutableStateOf(1) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var errorNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var primerPin by rememberSaveable { mutableStateOf("") }

    when (paso) {
        1 -> PantallaCentrada {
            Titulo("Te damos la bienvenida")
            TextoSuave("Esta app lleva el control de vacaciones, citas médicas y permisos. Todo queda guardado en este celular.")
            Campo(
                etiqueta = "¿Cómo te llamás?",
                valor = nombre,
                alCambiar = { nombre = it; errorNombre = null },
                ayuda = "Por ejemplo: Brenda Vásquez. Se usa para saludarte.",
                error = errorNombre,
                mayusculas = KeyboardCapitalization.Words,
            )
            BotonPrimario("Continuar", {
                alcance.launch {
                    when (val r = intentar { contenedor.repositorio.guardarNombreUsuaria(nombre) }) {
                        is Resultado.Bien -> paso = 2
                        is Resultado.Mal -> errorNombre = r.mensaje
                    }
                }
            }, Modifier.fillMaxWidth())
            TextoSuave("¿Ya usabas la app en otro celular? Después de crear tu PIN vas a poder traer tus datos desde «Respaldo».")
        }

        2 -> TecladoPin(
            titulo = "Creá tu PIN",
            explicacion = PoliticaPin.EXPLICACION + " Lo vas a usar cada vez que abras la app.",
            alCompletar = { pin, mostrarError ->
                val problema = PoliticaPin.revisar(pin)
                if (problema != null) mostrarError(problema) else {
                    primerPin = pin
                    paso = 3
                }
            },
        )

        else -> TecladoPin(
            titulo = "Repetí tu PIN",
            explicacion = "Escribilo otra vez para confirmar que no hubo un error de dedo.",
            alVolver = { primerPin = ""; paso = 2 },
            alCompletar = { pin, mostrarError ->
                if (pin != primerPin) {
                    mostrarError("Los dos PIN no coinciden. Volvé a crearlo.")
                    primerPin = ""
                    paso = 2
                } else {
                    alcance.launch {
                        withContext(Dispatchers.Default) { contenedor.acceso.crearPin(pin) }
                        contenedor.repositorio.anotar(AccionBitacora.PIN_CREADO, null)
                        contenedor.sesion.desbloquear()
                        alTerminar()
                    }
                }
            },
        )
    }
}

/** Pide el PIN para entrar. 5 equivocaciones bloquean 15 minutos. */
@Composable
fun PantallaDesbloqueo(nombre: String) {
    val contenedor = LocalContenedor.current
    val alcance = rememberCoroutineScope()
    var mensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var minutosBloqueo by rememberSaveable { mutableStateOf(0L) }
    var olvido by rememberSaveable { mutableStateOf(false) }

    // Revisa cada 15 segundos si ya pasó el bloqueo, para actualizar la pantalla.
    LaunchedEffect(Unit) {
        while (true) {
            val estado = contenedor.acceso.estado()
            minutosBloqueo = estado.minutosRestantes(java.time.Instant.now())
            delay(15_000)
        }
    }

    TecladoPin(
        titulo = if (nombre.isBlank()) "Escribí tu PIN" else "Hola, ${nombre.substringBefore(' ')}",
        explicacion = "Escribí tu PIN de 4 números para entrar.",
        bloqueado = minutosBloqueo > 0,
        mensajeExterno = when {
            minutosBloqueo > 0 -> "Por seguridad la app se bloqueó después de 5 intentos equivocados. " +
                "Probá de nuevo en $minutosBloqueo ${if (minutosBloqueo == 1L) "minuto" else "minutos"}."
            else -> mensaje
        },
        alCompletar = { pin, _ ->
            alcance.launch {
                val resultado = withContext(Dispatchers.Default) { contenedor.acceso.verificar(pin) }
                when (resultado) {
                    ResultadoPin.Correcto -> {
                        mensaje = null
                        contenedor.sesion.desbloquear()
                    }
                    is ResultadoPin.Incorrecto -> {
                        contenedor.repositorio.anotar(AccionBitacora.INGRESO_FALLIDO, null)
                        mensaje = "PIN equivocado. " + if (resultado.intentosQueQuedan == 1) "Te queda 1 intento antes de que la app se bloquee 15 minutos."
                        else "Te quedan ${resultado.intentosQueQuedan} intentos."
                    }
                    is ResultadoPin.RecienBloqueado -> {
                        contenedor.repositorio.anotar(AccionBitacora.INGRESO_FALLIDO, null)
                        contenedor.repositorio.anotar(AccionBitacora.BLOQUEO_POR_INTENTOS, "Bloqueada por ${resultado.minutos} minutos")
                        minutosBloqueo = resultado.minutos
                    }
                    is ResultadoPin.Bloqueado -> minutosBloqueo = resultado.minutos
                }
            }
        },
        pie = {
            BotonTexto("¿Olvidaste tu PIN?", { olvido = !olvido })
            if (olvido) {
                Aviso(
                    "El PIN no se puede recuperar desde la app: es lo que protege los datos. Si tenés un respaldo, " +
                        "quien te instaló la app puede reinstalarla y traer tus datos desde ese archivo. Sin respaldo, " +
                        "los datos de este celular no se pueden abrir.",
                    TipoAviso.INFORMACION,
                )
            }
        },
    )
}

/**
 * El teclado numérico grande con los 4 puntos arriba. Se usa para crear,
 * repetir y escribir el PIN.
 *
 * @param alCompletar recibe el PIN de 4 dígitos y una función para mostrar un error
 */
@Composable
fun TecladoPin(
    titulo: String,
    explicacion: String,
    alCompletar: (pin: String, mostrarError: (String) -> Unit) -> Unit,
    bloqueado: Boolean = false,
    mensajeExterno: String? = null,
    alVolver: (() -> Unit)? = null,
    pie: @Composable () -> Unit = {},
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun tocar(digito: Char) {
        if (bloqueado || pin.length >= PoliticaPin.LARGO) return
        error = null
        pin += digito
        if (pin.length == PoliticaPin.LARGO) {
            val completo = pin
            pin = ""
            alCompletar(completo) { error = it }
        }
    }

    PantallaCentrada {
        Text(
            titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = Paleta.Texto,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        TextoSuave(explicacion, Modifier.fillMaxWidth())

        // Los 4 puntos. Para el lector de pantalla: "2 de 4 números escritos".
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(vertical = 8.dp).semantics(mergeDescendants = true) {
                contentDescription = "${pin.length} de ${PoliticaPin.LARGO} números escritos"
            },
        ) {
            repeat(PoliticaPin.LARGO) { i ->
                val lleno = i < pin.length
                Box(
                    Modifier
                        .size(20.dp)
                        .background(if (lleno) Paleta.Primario else Paleta.Superficie, CircleShape)
                        .border(2.dp, if (lleno) Paleta.Primario else Paleta.Suave, CircleShape),
                )
            }
        }

        (error ?: mensajeExterno)?.let { Aviso(it, if (bloqueado) TipoAviso.ADVERTENCIA else TipoAviso.ERROR) }

        val filas = listOf("123", "456", "789")
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                fila.forEach { d -> TeclaPin(d.toString(), habilitada = !bloqueado) { tocar(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(76.dp))
            TeclaPin("0", habilitada = !bloqueado) { tocar('0') }
            TeclaPin("⌫", habilitada = pin.isNotEmpty(), descripcion = "Borrar") { pin = pin.dropLast(1) }
        }
        if (alVolver != null) BotonTexto("Volver a empezar", alVolver)
        Spacer(Modifier.height(4.dp))
        pie()
    }
}

@Composable
private fun TeclaPin(texto: String, habilitada: Boolean, descripcion: String = texto, alTocar: () -> Unit) {
    Surface(
        onClick = alTocar,
        enabled = habilitada,
        shape = CircleShape,
        color = if (habilitada) Paleta.Superficie else Paleta.Fondo,
        contentColor = if (habilitada) Paleta.Texto else Paleta.Suave,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (habilitada) Paleta.Suave else Paleta.Elevada),
        modifier = Modifier.size(76.dp).semantics {
            contentDescription = descripcion
            role = Role.Button
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(texto, fontSize = 28.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}
