package ni.gestionvacaciones.movil.ui.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ni.gestionvacaciones.movil.reporte.ArchivoGenerado
import ni.gestionvacaciones.movil.ui.LocalAvisar
import ni.gestionvacaciones.movil.ui.tema.Paleta
import java.io.File

/**
 * Ofrece un archivo recién generado (reporte o respaldo) con dos caminos:
 *
 * - **Compartir**: abre el menú de Android para mandarlo por WhatsApp, correo,
 *   Drive... El archivo se pasa por un FileProvider, que da permiso de lectura
 *   solo a la app elegida y solo para ese archivo.
 * - **Guardar en el teléfono**: abre el selector de Android para elegir carpeta
 *   (Descargas, Drive...). La app no necesita permisos de almacenamiento.
 */
class Exportador internal constructor(private val pendiente: MutableState<ArchivoGenerado?>) {
    fun ofrecer(archivo: ArchivoGenerado) {
        pendiente.value = archivo
    }
}

/**
 * @param alEntregar se llama cuando el archivo se compartió o se guardó
 *        (por ejemplo, para anotar en la bitácora la fecha del último respaldo)
 */
@Composable
fun rememberExportador(
    titulo: String,
    explicacion: String,
    alEntregar: (ArchivoGenerado) -> Unit = {},
): Exportador {
    val contexto = LocalContext.current
    val avisar = LocalAvisar.current
    val pendiente = remember { mutableStateOf<ArchivoGenerado?>(null) }
    val enGuardado = remember { mutableStateOf<ArchivoGenerado?>(null) }

    // Un selector por tipo de archivo: Android lo exige al registrarlo.
    fun alElegirDestino(uri: android.net.Uri?) {
        val archivo = enGuardado.value ?: return
        enGuardado.value = null
        if (uri == null) return // canceló
        val bien = runCatching {
            contexto.contentResolver.openOutputStream(uri)?.use { it.write(archivo.contenido) } ?: error("sin flujo")
        }.onFailure { Log.e("Vacaciones", "No se pudo guardar el archivo", it) }.isSuccess
        if (bien) {
            avisar("Listo: «${archivo.nombre}» quedó guardado.")
            alEntregar(archivo)
        } else {
            avisar("No se pudo guardar el archivo en ese lugar. Probá con otra carpeta o usá «Compartir».")
        }
    }
    val guardarExcel = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { alElegirDestino(it) }
    val guardarCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { alElegirDestino(it) }
    val guardarJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { alElegirDestino(it) }

    pendiente.value?.let { archivo ->
        AlertDialog(
            onDismissRequest = { pendiente.value = null },
            containerColor = Paleta.Superficie,
            titleContentColor = Paleta.Texto,
            textContentColor = Paleta.Texto,
            title = { Text(titulo) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(explicacion, style = MaterialTheme.typography.bodyLarge)
                    Text("Archivo: ${archivo.nombre}", color = Paleta.Suave)
                    BotonPrimario("Compartir", {
                        pendiente.value = null
                        if (compartir(contexto, archivo)) alEntregar(archivo)
                        else avisar("No hay ninguna app para compartir el archivo. Probá con «Guardar en el teléfono».")
                    }, Modifier.fillMaxWidth(), icono = "↗")
                    BotonSecundario("Guardar en el teléfono", {
                        pendiente.value = null
                        enGuardado.value = archivo
                        when {
                            archivo.nombre.endsWith(".xlsx") -> guardarExcel.launch(archivo.nombre)
                            archivo.nombre.endsWith(".csv") -> guardarCsv.launch(archivo.nombre)
                            else -> guardarJson.launch(archivo.nombre)
                        }
                    }, Modifier.fillMaxWidth(), icono = "⬇")
                }
            },
            confirmButton = {},
            dismissButton = { BotonTexto("Cerrar", { pendiente.value = null }) },
        )
    }
    return remember { Exportador(pendiente) }
}

/** @return false si no hay ninguna app que pueda recibir el archivo */
private fun compartir(contexto: Context, archivo: ArchivoGenerado): Boolean {
    val carpeta = File(contexto.cacheDir, "compartidos").apply { mkdirs() }
    // Higiene: se borran los archivos compartidos de más de una hora.
    carpeta.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 3_600_000 }?.forEach { it.delete() }

    val destino = File(carpeta, archivo.nombre).apply { writeBytes(archivo.contenido) }
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", destino)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = archivo.tipoContenido
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, archivo.nombre)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        contexto.startActivity(Intent.createChooser(envio, "Compartir ${archivo.nombre}"))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
