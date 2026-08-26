package com.wuelmer.vidaos.ui.registrargasto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.CategoriaDao
import com.wuelmer.vidaos.data.Movimiento
import com.wuelmer.vidaos.data.MovimientoDao
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoCategoria
import com.wuelmer.vidaos.data.TipoMovimiento
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegistrarGastoViewModel(
    private val movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrarGastoUiState())
    val uiState: StateFlow<RegistrarGastoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoriaDao.getAll().collect { categorias ->
                _uiState.update { estado ->
                    estado.copy(
                        categorias = categorias,
                        categoriaId = estado.categoriaId ?: categorias.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onMontoChange(monto: String) {
        _uiState.update { it.copy(monto = monto, errorMonto = false) }
    }

    fun onDescripcionChange(descripcion: String) {
        _uiState.update { it.copy(descripcion = descripcion) }
    }

    fun onFechaChange(fecha: LocalDate) {
        _uiState.update { it.copy(fecha = fecha) }
    }

    fun onOrigenChange(origen: OrigenPago) {
        _uiState.update { it.copy(origen = origen) }
    }

    fun onTipoChange(tipo: TipoMovimiento) {
        _uiState.update { estado ->
            val tipoCategoria = tipo.categoriaCorrespondiente()
            val categoriaSigueSiendoValida = tipoCategoria == null ||
                estado.categorias.find { it.id == estado.categoriaId }?.tipo == tipoCategoria
            estado.copy(
                tipo = tipo,
                categoriaId = if (categoriaSigueSiendoValida) {
                    estado.categoriaId
                } else {
                    estado.categorias.firstOrNull { it.tipo == tipoCategoria }?.id
                }
            )
        }
    }

    fun onCategoriaChange(categoriaId: Long) {
        _uiState.update { it.copy(categoriaId = categoriaId) }
    }

    fun agregarCategoria(nombre: String, tipo: TipoCategoria) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            val nueva = categoriaDao.agregar(nombre, tipo)
            _uiState.update { it.copy(categoriaId = nueva.id) }
        }
    }

    fun onGuardadoExitosoConsumido() {
        _uiState.update { it.copy(guardadoExitoso = false) }
    }

    fun guardar() {
        val estado = _uiState.value
        val monto = estado.monto.toLongOrNull()
        val categoriaId = estado.categoriaId

        if (monto == null || monto <= 0) {
            _uiState.update { it.copy(errorMonto = true) }
            return
        }
        if (categoriaId == null) {
            return
        }

        viewModelScope.launch {
            movimientoDao.insert(
                Movimiento(
                    fecha = estado.fecha,
                    monto = monto,
                    descripcion = estado.descripcion,
                    categoriaId = categoriaId,
                    origen = estado.origen,
                    tipo = estado.tipo
                )
            )
            _uiState.update {
                RegistrarGastoUiState(
                    categorias = it.categorias,
                    categoriaId = it.categoriaId,
                    guardadoExitoso = true
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                RegistrarGastoViewModel(
                    movimientoDao = application.database.movimientoDao(),
                    categoriaDao = application.database.categoriaDao()
                )
            }
        }
    }
}
