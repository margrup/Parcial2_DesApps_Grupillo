package ar.jornadas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.jornadas.data.Charla

@Composable
fun Catalogo(model: ProgramaModel, abrir: (String) -> Unit) {
    val dia by model.dia.collectAsStateWithLifecycle()
    val tema by model.tema.collectAsStateWithLifecycle()
    val uiState by model.uiState.collectAsStateWithLifecycle()

    val temas = listOf("Todos", "Android", "Datos", "Calidad")
    Selector(listOf("Ambos días", "Día 1", "Día 2"), dia) { model.setDia(it) }
    Selector(temas, temas.indexOf(tema)) { model.setTema(temas[it]) }

    when (val state = uiState) {
        is UiState.Cargando -> {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        is UiState.SinEdicion -> {
            Text(if (state.fallo) "No se pudo consultar el programa." else "Sin edición descargada.")
            OutlinedButton(onClick = model::recargar) {
                Text("Reintentar")
            }
        }
        is UiState.Programa -> {
            TextButton(
                onClick = model::recargar,
                enabled = !state.cargando
            ) {
                Text("Actualizar")
            }
            if (state.cargando) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (state.aviso.isNotEmpty()) {
                Text(state.aviso)
            }
            if (state.todas.isEmpty()) {
                Text("sin charlas programadas")
            } else if (state.charlas.isEmpty()) {
                Text("sin resultados para el filtro")
            } else {
                Grilla(state.charlas, abrir)
            }
        }
    }
}

@Composable
private fun Selector(opciones: List<String>, indice: Int, elegir: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opciones.forEachIndexed { i, texto ->
            FilterChip(selected = i == indice, onClick = { elegir(i) }, label = { Text(texto) })
        }
    }
}

@Composable
fun Grilla(fichas: List<Charla>, abrir: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)) {
        items(fichas, key = { it.id }) { ficha ->
            ElevatedCard(onClick = { abrir(ficha.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}", style = MaterialTheme.typography.labelMedium)
                    Text(ficha.titulo, style = MaterialTheme.typography.titleLarge)
                    Text(if (ficha.favorita) "${ficha.tema} · En mi agenda" else ficha.tema)
                }
            }
        }
    }
}
