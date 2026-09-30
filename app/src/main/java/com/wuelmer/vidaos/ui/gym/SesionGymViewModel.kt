package com.wuelmer.vidaos.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.wuelmer.vidaos.data.BorradorSerie
import com.wuelmer.vidaos.data.PreferenciasRepository
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.SesionGym
import com.wuelmer.vidaos.data.VidaOSDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Compartido entre instancias: al salir y volver a entrar, la carga espera a que termine la última escritura.
private val mutexBorrador = Mutex()

class SesionGymViewModel(
    private val diaRutinaId: Long,
    private val database: VidaOSDatabase,
    private val preferencias: PreferenciasRepository,
    // El borrador se escribe fuera del viewModelScope para que no se pierda la última tecla al salir.
    private val appScope: CoroutineScope
) : ViewModel() {

    private val gymDao = database.gymDao()

    private val _uiState = MutableStateFlow(SesionGymUiState())
    val uiState: StateFlow<SesionGymUiState> = _uiState.asStateFlow()

    private var iniciales: List<EjercicioSesionUi> = emptyList()

    @Volatile
    private var finalizada = false

    init {
        viewModelScope.launch {
            val dia = gymDao.getDia(diaRutinaId)
            val unidad = preferencias.preferencias.first().unidadPeso
            iniciales = gymDao.getEjerciciosDelDia(diaRutinaId).map { e ->
                val opciones = listOfNotNull(e.ejercicio, e.alternativa)
                EjercicioSesionUi(
                    rutinaEjercicioId = e.objetivo.id,
                    opciones = opciones,
                    elegidoId = e.ejercicio.id,
                    series = e.objetivo.series,
                    objetivoMin = e.objetivo.objetivoMin,
                    objetivoMax = e.objetivo.objetivoMax,
                    descansoSegundos = e.objetivo.descansoSegundos,
                    inputs = List(e.objetivo.series) { SerieInput() },
                    historial = opciones.associate { it.id to gymDao.getSeriesUltimaVez(it.id) },
                    unidad = unidad
                )
            }
            val borrador = mutexBorrador.withLock { gymDao.getBorrador(diaRutinaId) }
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombreDia = dia?.nombre.orEmpty(),
                    unidad = unidad,
                    ejercicios = aplicarBorrador(iniciales, borrador),
                    recuperada = borrador.isNotEmpty()
                )
            }
        }
    }

    fun onEjercicioElegido(rutinaEjercicioId: Long, ejercicioId: Long) =
        actualizar(rutinaEjercicioId) { it.copy(elegidoId = ejercicioId) }

    fun onAgregarSerie(rutinaEjercicioId: Long) =
        actualizar(rutinaEjercicioId) { it.copy(inputs = it.inputs + SerieInput()) }

    fun onEliminarSerie(rutinaEjercicioId: Long, indice: Int) =
        actualizar(rutinaEjercicioId) { ejercicio ->
            if (ejercicio.inputs.size <= 1) ejercicio
            else ejercicio.copy(inputs = ejercicio.inputs.filterIndexed { i, _ -> i != indice })
        }

    fun onRepsChange(rutinaEjercicioId: Long, indice: Int, valor: String) =
        modificarSerie(rutinaEjercicioId, indice) { it.copy(reps = valor.filter(Char::isDigit).take(3)) }

    fun onSegundosChange(rutinaEjercicioId: Long, indice: Int, valor: String) =
        modificarSerie(rutinaEjercicioId, indice) { it.copy(segundos = valor.filter(Char::isDigit).take(4)) }

    fun onPesoChange(rutinaEjercicioId: Long, indice: Int, valor: String) =
        modificarSerie(rutinaEjercicioId, indice) {
            it.copy(peso = valor.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6))
        }

    fun descartarBorrador() {
        appScope.launch {
            mutexBorrador.withLock {
                gymDao.deleteBorrador(diaRutinaId)
                _uiState.update { estado ->
                    estado.copy(
                        ejercicios = iniciales.map { it.copy(inputs = List(it.series) { SerieInput() }) },
                        recuperada = false,
                        mostrarErrores = false,
                        errorSinSeries = false
                    )
                }
            }
        }
    }

    fun guardar(onGuardado: () -> Unit) {
        val estado = _uiState.value
        if (estado.guardando) return

        var hayInvalida = false
        val series = mutableListOf<SerieGym>()
        var orden = 1
        estado.ejercicios.forEach { ejercicio ->
            ejercicio.inputs.forEach { input ->
                when (val resultado = input.evaluar(ejercicio.elegido.tipo)) {
                    ResultadoSerie.Vacia -> Unit
                    ResultadoSerie.Invalida -> hayInvalida = true
                    is ResultadoSerie.Valida -> series += SerieGym(
                        sesionId = 0,
                        ejercicioId = ejercicio.elegidoId,
                        orden = orden++,
                        repeticiones = resultado.repeticiones,
                        segundos = resultado.segundos,
                        pesoKg = resultado.peso?.let(estado.unidad::aKg)
                    )
                }
            }
        }

        if (hayInvalida) {
            _uiState.update { it.copy(mostrarErrores = true, errorSinSeries = false) }
            return
        }
        if (series.isEmpty()) {
            _uiState.update { it.copy(errorSinSeries = true, mostrarErrores = false) }
            return
        }

        _uiState.update { it.copy(guardando = true, mostrarErrores = false, errorSinSeries = false) }
        viewModelScope.launch {
            mutexBorrador.withLock {
                database.withTransaction {
                    val sesionId = gymDao.insertSesion(SesionGym(fecha = estado.fecha, diaRutinaId = diaRutinaId))
                    gymDao.insertSeries(series.map { it.copy(sesionId = sesionId) })
                    gymDao.deleteBorrador(diaRutinaId)
                }
                finalizada = true
            }
            onGuardado()
        }
    }

    private fun actualizar(rutinaEjercicioId: Long, transformar: (EjercicioSesionUi) -> EjercicioSesionUi) {
        _uiState.update { estado ->
            estado.copy(
                ejercicios = estado.ejercicios.map {
                    if (it.rutinaEjercicioId == rutinaEjercicioId) transformar(it) else it
                },
                errorSinSeries = false
            )
        }
        guardarBorrador()
    }

    private fun modificarSerie(rutinaEjercicioId: Long, indice: Int, transformar: (SerieInput) -> SerieInput) {
        actualizar(rutinaEjercicioId) { ejercicio ->
            ejercicio.copy(
                inputs = ejercicio.inputs.mapIndexed { i, input -> if (i == indice) transformar(input) else input }
            )
        }
    }

    // Cada escritura toma el estado vigente al obtener el candado, así la última siempre refleja lo último escrito.
    private fun guardarBorrador() {
        appScope.launch {
            mutexBorrador.withLock {
                val estado = _uiState.value
                if (finalizada || estado.cargando) return@withLock
                val filas = aBorrador(diaRutinaId, estado.ejercicios)
                database.withTransaction {
                    gymDao.deleteBorrador(diaRutinaId)
                    if (filas.isNotEmpty()) gymDao.insertBorrador(filas)
                }
            }
        }
    }
}

// Sin nada escrito ni alternativa elegida no hay borrador que guardar (devuelve lista vacía).
fun aBorrador(diaRutinaId: Long, ejercicios: List<EjercicioSesionUi>): List<BorradorSerie> {
    val hayDatos = ejercicios.any { ejercicio ->
        ejercicio.elegidoId != ejercicio.opciones.first().id ||
            ejercicio.inputs.size != ejercicio.series ||
            ejercicio.inputs.any { it.reps.isNotBlank() || it.peso.isNotBlank() || it.segundos.isNotBlank() }
    }
    if (!hayDatos) return emptyList()
    return ejercicios.flatMap { ejercicio ->
        ejercicio.inputs.mapIndexed { i, input ->
            BorradorSerie(
                diaRutinaId = diaRutinaId,
                rutinaEjercicioId = ejercicio.rutinaEjercicioId,
                ejercicioId = ejercicio.elegidoId,
                orden = i,
                reps = input.reps,
                peso = input.peso,
                segundos = input.segundos
            )
        }
    }
}

fun aplicarBorrador(ejercicios: List<EjercicioSesionUi>, borrador: List<BorradorSerie>): List<EjercicioSesionUi> {
    val porEjercicio = borrador.groupBy { it.rutinaEjercicioId }
    return ejercicios.map { ejercicio ->
        val filas = porEjercicio[ejercicio.rutinaEjercicioId]?.sortedBy { it.orden } ?: return@map ejercicio
        val elegido = filas.first().ejercicioId.takeIf { id -> ejercicio.opciones.any { it.id == id } }
        ejercicio.copy(
            elegidoId = elegido ?: ejercicio.elegidoId,
            inputs = filas.map { SerieInput(reps = it.reps, peso = it.peso, segundos = it.segundos) }
        )
    }
}
