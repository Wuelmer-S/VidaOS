package com.wuelmer.vidaos.ui.configuracion

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.avisos.AvisosReceiver
import com.wuelmer.vidaos.data.HORA_AVISOS_MOTO_DEFECTO
import com.wuelmer.vidaos.data.HORA_RECORDATORIO_GASTOS_DEFECTO
import com.wuelmer.vidaos.data.META_SEMANAL_DEFECTO
import com.wuelmer.vidaos.data.PreferenciasRepository
import com.wuelmer.vidaos.data.RespaldoRepository
import com.wuelmer.vidaos.data.ResultadoRespaldo
import com.wuelmer.vidaos.data.Tema
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.puedeOcultar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfiguracionUiState(
    val cargando: Boolean = true,
    val tema: Tema = Tema.SISTEMA,
    val modulosOcultos: Set<String> = emptySet(),
    val unidadPeso: UnidadPeso = UnidadPeso.KG,
    val metaSemanal: Int = META_SEMANAL_DEFECTO,
    val avisosMoto: Boolean = true,
    val horaAvisosMoto: Int = HORA_AVISOS_MOTO_DEFECTO,
    val recordatorioGastos: Boolean = true,
    val horaRecordatorioGastos: Int = HORA_RECORDATORIO_GASTOS_DEFECTO
)

class ConfiguracionViewModel(
    private val preferencias: PreferenciasRepository,
    private val respaldo: RespaldoRepository,
    // Corre la revisión de avisos en el momento (botón "Revisar ahora").
    private val revisarAvisos: suspend () -> Unit
) : ViewModel() {

    // Resultado de la última operación de respaldo, para mostrarlo en pantalla.
    private val _mensajeRespaldo = MutableStateFlow<String?>(null)
    val mensajeRespaldo: StateFlow<String?> = _mensajeRespaldo.asStateFlow()

    private val _procesandoRespaldo = MutableStateFlow(false)
    val procesandoRespaldo: StateFlow<Boolean> = _procesandoRespaldo.asStateFlow()

    val uiState: StateFlow<ConfiguracionUiState> = preferencias.preferencias
        .map {
            ConfiguracionUiState(
                cargando = false,
                tema = it.tema,
                modulosOcultos = it.modulosOcultos,
                unidadPeso = it.unidadPeso,
                metaSemanal = it.metaSemanal,
                avisosMoto = it.avisosMoto,
                horaAvisosMoto = it.horaAvisosMoto,
                recordatorioGastos = it.recordatorioGastos,
                horaRecordatorioGastos = it.horaRecordatorioGastos
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConfiguracionUiState()
        )

    fun onTemaChange(tema: Tema) {
        viewModelScope.launch { preferencias.setTema(tema) }
    }

    fun onMetaSemanalChange(meta: Int) {
        viewModelScope.launch { preferencias.setMetaSemanal(meta) }
    }

    fun onUnidadPesoChange(unidad: UnidadPeso) {
        viewModelScope.launch { preferencias.setUnidadPeso(unidad) }
    }

    // Al cambiar, VidaOSApplication reprograma las alarmas sola (escucha las preferencias).
    fun onAvisosMotoChange(activos: Boolean) {
        viewModelScope.launch { preferencias.setAvisosMoto(activos) }
    }

    fun onHoraAvisosMotoChange(minutos: Int) {
        viewModelScope.launch { preferencias.setHoraAvisosMoto(minutos) }
    }

    fun onRecordatorioGastosChange(activo: Boolean) {
        viewModelScope.launch { preferencias.setRecordatorioGastos(activo) }
    }

    fun onHoraRecordatorioGastosChange(minutos: Int) {
        viewModelScope.launch { preferencias.setHoraRecordatorioGastos(minutos) }
    }

    fun revisarAvisosAhora() {
        viewModelScope.launch { revisarAvisos() }
    }

    fun exportarRespaldo(destino: Uri) = operarRespaldo {
        when (val r = respaldo.exportar(destino)) {
            ResultadoRespaldo.Ok -> "Respaldo guardado."
            is ResultadoRespaldo.Error -> r.mensaje
        }
    }

    // Si sale bien la app se reinicia sola; aquí solo llegan los errores.
    fun restaurarRespaldo(origen: Uri) = operarRespaldo {
        (respaldo.restaurar(origen) as? ResultadoRespaldo.Error)?.mensaje
    }

    fun onMensajeRespaldoVisto() {
        _mensajeRespaldo.value = null
    }

    private fun operarRespaldo(operacion: suspend () -> String?) {
        if (_procesandoRespaldo.value) return
        _procesandoRespaldo.value = true
        viewModelScope.launch {
            _mensajeRespaldo.value = operacion()
            _procesandoRespaldo.value = false
        }
    }

    fun onModuloVisibleChange(modulo: Modulo, visible: Boolean) {
        if (!visible && !puedeOcultar(modulo, uiState.value.modulosOcultos)) return
        viewModelScope.launch { preferencias.setModuloOculto(modulo.name, oculto = !visible) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                ConfiguracionViewModel(
                    preferencias = application.preferencias,
                    respaldo = application.respaldo,
                    revisarAvisos = {
                        AvisosReceiver.revisarMoto(application)
                        AvisosReceiver.revisarGastos(application)
                    }
                )
            }
        }
    }
}
