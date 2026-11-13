package ar.jornadas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun Portada(model: ProgramaModel) {
    var seccion by rememberSaveable { mutableStateOf("Programa") }
    var detalle by rememberSaveable { mutableStateOf<String?>(null) }
    val uiState by model.uiState.collectAsStateWithLifecycle()
    BackHandler(detalle != null || seccion != "Programa") {
        if (detalle != null) detalle = null else seccion = "Programa"
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF185B54), secondary = Color(0xFF995136))) {
        Scaffold(bottomBar = {
            if (detalle == null) NavigationBar {
                listOf("Programa" to "▤", "Mi agenda" to "☆", "Mesa" to "⚙").forEach { (nombre, icono) ->
                    NavigationBarItem(seccion == nombre, { seccion = nombre },
                        icon = { Text(icono) }, label = { Text(nombre) })
                }
            }
        }) { margen ->
            Column(Modifier.fillMaxSize().padding(margen).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("Jornadas del Sur", style = MaterialTheme.typography.labelLarge)
                if (detalle != null) {
                    TextButton(onClick = { detalle = null }) { Text("Volver") }
                    if (uiState is UiState.Cargando) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        val ficha = (uiState as? UiState.Programa)?.todas?.find { it.id == detalle }
                        if (ficha != null) {
                            Column(Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(ficha.titulo, style = MaterialTheme.typography.headlineMedium)
                                Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}")
                                Text(ficha.ponente, style = MaterialTheme.typography.titleMedium)
                                Text(ficha.resumen)
                                Button(onClick = { model.marcar(ficha.id) }) {
                                    Text(if (ficha.favorita) "Quitar de mi agenda" else "Agregar a mi agenda")
                                }
                            }
                        } else {
                            Text("La charla seleccionada ya no existe en el catálogo.")
                        }
                    }
                } else {
                    Text(seccion, style = MaterialTheme.typography.headlineLarge)
                    when (seccion) {
                        "Programa" -> Catalogo(model) { detalle = it }
                        "Mi agenda" -> {
                            when (val state = uiState) {
                                is UiState.Cargando -> {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                }
                                is UiState.SinEdicion -> {
                                    Text(if (state.fallo) "No se pudo consultar el programa." else "Sin edición descargada.")
                                    OutlinedButton(onClick = model::recargar) { Text("Reintentar") }
                                }
                                is UiState.Programa -> {
                                    if (state.aviso.isNotEmpty()) {
                                        Text(state.aviso)
                                    }
                                    val guardadas = state.todas.filter { it.favorita }
                                    if (guardadas.isEmpty()) {
                                        Text("Todavía no agregaste charlas.")
                                    } else {
                                        Grilla(guardadas) { detalle = it }
                                    }
                                }
                            }
                        }
                        else -> Mesa(model.mesa)
                    }
                }
            }
        }
    }
}
