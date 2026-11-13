package ar.jornadas.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa: Flow<List<Charla>> = dao.observar()
    val edicionDescargada: Flow<Boolean> = dao.observarEdicion().map { it == true }

    private val sincronizando = AtomicBoolean(false)
    private val cargaAutomaticaReclamada = AtomicBoolean(false)

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    fun reclamarCargaAutomatica(): Boolean =
        cargaAutomaticaReclamada.compareAndSet(false, true)

    suspend fun sincronizar(): Boolean {
        if (!sincronizando.compareAndSet(false, true)) return false
        _cargando.value = true
        try {
            val fichas = api.consultar().map {
                Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
            }
            dao.aplicarInstantanea(fichas)
            return true
        } finally {
            _cargando.value = false
            sincronizando.set(false)
        }
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
