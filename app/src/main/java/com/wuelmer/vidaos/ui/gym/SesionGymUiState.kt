package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.UnidadPeso
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

private val contadorSeries = AtomicLong()

data class SerieInput(
    val reps: String = "",
    val peso: String = "",
    val segundos: String = "",
    val id: Long = contadorSeries.incrementAndGet()
)

data class EjercicioSesionUi(
    val rutinaEjercicioId: Long,
    val opciones: List<EjercicioGym>,
    val elegidoId: Long,
    val series: Int,
    val objetivoMin: Int,
    val objetivoMax: Int,
    val descansoSegundos: Int,
    val inputs: List<SerieInput>,
    val historial: Map<Long, List<SerieGym>> = emptyMap(),
    val unidad: UnidadPeso = UnidadPeso.KG
) {
    val elegido: EjercicioGym get() = opciones.first { it.id == elegidoId }
    val ultimaVez: List<SerieGym> get() = historial[elegidoId].orEmpty()
    val sugerencia: Sugerencia? get() = sugerir(elegido, series, objetivoMax, ultimaVez, unidad)
}

data class SesionGymUiState(
    val cargando: Boolean = true,
    val nombreDia: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val unidad: UnidadPeso = UnidadPeso.KG,
    val ejercicios: List<EjercicioSesionUi> = emptyList(),
    val mostrarErrores: Boolean = false,
    val errorSinSeries: Boolean = false,
    val guardando: Boolean = false,
    // Se retomó una sesión que había quedado a medio llenar.
    val recuperada: Boolean = false
)

sealed interface ResultadoSerie {
    object Vacia : ResultadoSerie
    object Invalida : ResultadoSerie
    data class Valida(
        val repeticiones: Int?,
        val segundos: Int?,
        // En la unidad elegida; se convierte a kg al guardar.
        val peso: Double?
    ) : ResultadoSerie
}

fun SerieInput.evaluar(tipo: TipoEjercicio): ResultadoSerie {
    val repsNum = reps.toIntOrNull()?.takeIf { it > 0 }
    val segundosNum = segundos.toIntOrNull()?.takeIf { it > 0 }
    val pesoNum = peso.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    return when (tipo) {
        TipoEjercicio.CON_PESO -> when {
            reps.isBlank() && peso.isBlank() -> ResultadoSerie.Vacia
            repsNum != null && pesoNum != null -> ResultadoSerie.Valida(repsNum, null, pesoNum)
            else -> ResultadoSerie.Invalida
        }
        TipoEjercicio.PESO_CORPORAL -> when {
            reps.isBlank() -> ResultadoSerie.Vacia
            repsNum != null -> ResultadoSerie.Valida(repsNum, null, null)
            else -> ResultadoSerie.Invalida
        }
        TipoEjercicio.TIEMPO -> when {
            segundos.isBlank() -> ResultadoSerie.Vacia
            segundosNum != null -> ResultadoSerie.Valida(null, segundosNum, null)
            else -> ResultadoSerie.Invalida
        }
    }
}
