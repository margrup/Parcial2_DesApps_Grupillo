package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import ar.jornadas.data.Charla
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Cargando : UiState
    data class SinEdicion(val fallo: Boolean) : UiState
    data class Programa(
        val charlas: List<Charla>,
        val todas: List<Charla> = charlas,
        val cargando: Boolean = false,
        val aviso: String = ""
    ) : UiState
}

class ProgramaModel(
    app: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {
    private val servicios = Servicios.get(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda

    val dia: StateFlow<Int> = savedStateHandle.getStateFlow("dia", 0)
    val tema: StateFlow<String> = savedStateHandle.getStateFlow("tema", "Todos")

    fun setDia(nuevoDia: Int) {
        savedStateHandle["dia"] = nuevoDia
    }

    fun setTema(nuevoTema: String) {
        savedStateHandle["tema"] = nuevoTema
    }

    val cargando: StateFlow<Boolean> = repo.cargando

    private val _aviso = MutableStateFlow("")
    val aviso: StateFlow<String> = _aviso.asStateFlow()

    private val filtros = combine(dia, tema) { d, t -> Pair(d, t) }

    val uiState: StateFlow<UiState> = combine(
        repo.programa,
        repo.edicionDescargada,
        filtros,
        cargando,
        _aviso
    ) { todas, descargada, (d, t), enCarga, errorMsg ->
        if (!descargada) {
            if (enCarga) {
                UiState.Cargando
            } else {
                UiState.SinEdicion(fallo = errorMsg.isNotEmpty())
            }
        } else {
            val filtradas = todas.filter { (d == 0 || it.dia == d) && (t == "Todos" || it.tema == t) }
            UiState.Programa(
                charlas = filtradas,
                todas = todas,
                cargando = enCarga,
                aviso = errorMsg
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Cargando
    )

    init {
        if (repo.reclamarCargaAutomatica()) {
            recargar()
        }
    }

    fun recargar() {
        viewModelScope.launch {
            try {
                val sincronizo = repo.sincronizar()
                if (sincronizo) {
                    _aviso.value = ""
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aviso.value = "Sin conexión: se muestran datos guardados"
            }
        }
    }

    fun marcar(id: String) {
        viewModelScope.launch {
            repo.cambiarFavorita(id)
        }
    }
}
