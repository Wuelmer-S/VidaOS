package com.wuelmer.vidaos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wuelmer.vidaos.ui.movimientos.MovimientosRoute
import com.wuelmer.vidaos.ui.registrargasto.RegistrarGastoRoute
import com.wuelmer.vidaos.ui.theme.VidaOSTheme

private enum class VidaOSDestino(val ruta: String, val etiqueta: String) {
    REGISTRAR("registrar", "Registrar"),
    MOVIMIENTOS("movimientos", "Movimientos")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VidaOSTheme {
                VidaOSApp()
            }
        }
    }
}

@Composable
private fun VidaOSApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                VidaOSDestino.entries.forEach { destino ->
                    NavigationBarItem(
                        selected = rutaActual == destino.ruta,
                        onClick = {
                            navController.navigate(destino.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val icono = if (destino == VidaOSDestino.REGISTRAR) Icons.Filled.Add else Icons.AutoMirrored.Filled.List
                            Icon(icono, contentDescription = destino.etiqueta)
                        },
                        label = { Text(destino.etiqueta) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = VidaOSDestino.REGISTRAR.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(VidaOSDestino.REGISTRAR.ruta) { RegistrarGastoRoute() }
            composable(VidaOSDestino.MOVIMIENTOS.ruta) { MovimientosRoute() }
        }
    }
}
