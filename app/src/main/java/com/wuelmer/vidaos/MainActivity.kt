package com.wuelmer.vidaos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wuelmer.vidaos.ui.categorias.CategoriasRoute
import com.wuelmer.vidaos.ui.detalle.DetalleMovimientoRoute
import com.wuelmer.vidaos.ui.gym.GymRoute
import com.wuelmer.vidaos.ui.historial.HistorialRoute
import com.wuelmer.vidaos.ui.movimientos.MovimientosRoute
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.SelectorModulos
import com.wuelmer.vidaos.ui.registrargasto.RegistrarGastoRoute
import com.wuelmer.vidaos.ui.theme.VidaOSTheme

private enum class PestanaFinanzas(val ruta: String, val etiqueta: String) {
    REGISTRAR("registrar", "Registrar"),
    MOVIMIENTOS("movimientos", "Movimientos")
}

private const val RUTA_HISTORIAL = "historial"
private const val RUTA_CATEGORIAS = "categorias"
private const val ARG_MOVIMIENTO_ID = "movimientoId"
private const val RUTA_DETALLE = "detalle/{$ARG_MOVIMIENTO_ID}"

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
    val destinoActual = backStackEntry?.destination
    val rutaActual = destinoActual?.route

    val moduloActual = Modulo.entries.firstOrNull { modulo ->
        destinoActual?.hierarchy?.any { it.route == modulo.ruta } == true
    }
    val pestanaActual = PestanaFinanzas.entries.firstOrNull { it.ruta == rutaActual }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SelectorModulos(
                modulos = Modulo.entries,
                moduloActual = moduloActual,
                onModuloClick = { modulo -> navController.irAModulo(modulo) }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (pestanaActual != null) {
                PrimaryTabRow(selectedTabIndex = pestanaActual.ordinal) {
                    PestanaFinanzas.entries.forEach { pestana ->
                        Tab(
                            selected = pestana == pestanaActual,
                            onClick = { navController.irAPestana(pestana) },
                            text = { Text(pestana.etiqueta) }
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = Modulo.FINANZAS.ruta,
                modifier = Modifier.weight(1f)
            ) {
                navigation(
                    startDestination = PestanaFinanzas.REGISTRAR.ruta,
                    route = Modulo.FINANZAS.ruta
                ) {
                    composable(PestanaFinanzas.REGISTRAR.ruta) { RegistrarGastoRoute() }
                    composable(PestanaFinanzas.MOVIMIENTOS.ruta) {
                        MovimientosRoute(
                            onVerHistorialClick = { navController.navigate(RUTA_HISTORIAL) },
                            onMovimientoClick = { id -> navController.navigate("detalle/$id") },
                            onGestionarCategoriasClick = { navController.navigate(RUTA_CATEGORIAS) }
                        )
                    }
                    composable(RUTA_HISTORIAL) {
                        HistorialRoute(
                            onBackClick = { navController.popBackStack() },
                            onMovimientoClick = { id -> navController.navigate("detalle/$id") }
                        )
                    }
                    composable(RUTA_CATEGORIAS) {
                        CategoriasRoute(onBackClick = { navController.popBackStack() })
                    }
                    composable(
                        route = RUTA_DETALLE,
                        arguments = listOf(navArgument(ARG_MOVIMIENTO_ID) { type = NavType.LongType })
                    ) { entry ->
                        val movimientoId = entry.arguments?.getLong(ARG_MOVIMIENTO_ID) ?: 0L
                        DetalleMovimientoRoute(
                            movimientoId = movimientoId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
                composable(Modulo.GYM.ruta) { GymRoute() }
            }
        }
    }
}

private fun NavHostController.irAModulo(modulo: Modulo) {
    navigate(modulo.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.irAPestana(pestana: PestanaFinanzas) {
    navigate(pestana.ruta) {
        popUpTo(PestanaFinanzas.REGISTRAR.ruta) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
