package com.wuelmer.vidaos.ui.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.Categoria
import com.wuelmer.vidaos.data.CategoriaDao
import com.wuelmer.vidaos.data.MovimientoDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriasViewModel(
    private val categoriaDao: CategoriaDao,
    private val movimientoDao: MovimientoDao
) : ViewModel() {

    val categorias: StateFlow<List<Categoria>> = categoriaDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun eliminar(categoria: Categoria, onResultado: (exito: Boolean) -> Unit) {
        viewModelScope.launch {
            val enUso = movimientoDao.contarPorCategoria(categoria.id) > 0
            if (enUso) {
                onResultado(false)
            } else {
                categoriaDao.delete(categoria)
                onResultado(true)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                CategoriasViewModel(
                    categoriaDao = application.database.categoriaDao(),
                    movimientoDao = application.database.movimientoDao()
                )
            }
        }
    }
}
