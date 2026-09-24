package com.wuelmer.vidaos.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.SesionGym
import com.wuelmer.vidaos.data.VidaOSDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SesionGymViewModel(
    private val diaRutinaId: Long,
    private val database: VidaOSDatabase
) : ViewModel() {

    private val gymDao = database.gymDao()

    private val _uiState = MutableStateFlow(SesionGymUiState())
    val uiState: StateFlow<SesionGymUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dia = gymDao.getDia(diaRutinaId)
            val ejercicios = gymDao.getEjerciciosDelDia(diaRutinaId).map { e ->
                EjercicioSesionUi(
                    rutinaEjercicioId = e.objetivo.id,
                    opciones = listOfNotNull(e.ejercicio, e.alternativa),
                    elegidoId = e.ejercicio.id,
                    series = e.objetivo.series,
                    objetivoMin = e.objetivo.objetivoMin,
                    objetivoMax = e.objetivo.objetivoMax,
                    descansoSegundos = e.objetivo.descansoSegundos,
                    inputs = List(e.objetivo.series) { SerieInput() }
                )
            }
            _uiState.update {
                it.copy(cargando = false, nombreDia = dia?.nombre.orEmpty(), ejercicios = ejercicios)
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
                        pesoKg = resultado.pesoKg
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
            database.withTransaction {
                val sesionId = gymDao.insertSesion(SesionGym(fecha = estado.fecha, diaRutinaId = diaRutinaId))
                gymDao.insertSeries(series.map { it.copy(sesionId = sesionId) })
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
    }

    private fun modificarSerie(rutinaEjercicioId: Long, indice: Int, transformar: (SerieInput) -> SerieInput) {
        actualizar(rutinaEjercicioId) { ejercicio ->
            ejercicio.copy(
                inputs = ejercicio.inputs.mapIndexed { i, input -> if (i == indice) transformar(input) else input }
            )
        }
    }
}
