package com.wuelmer.vidaos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.wuelmer.vidaos.data.Tema
import com.wuelmer.vidaos.ui.categorias.CategoriasRoute
import com.wuelmer.vidaos.ui.configuracion.ConfiguracionRoute
import com.wuelmer.vidaos.ui.detalle.DetalleMovimientoRoute
import com.wuelmer.vidaos.ui.gym.DetalleSesionGymRoute
import com.wuelmer.vidaos.ui.gym.GymRoute
import com.wuelmer.vidaos.ui.gym.ProgresoGymRoute
import com.wuelmer.vidaos.ui.gym.SesionGymRoute
import com.wuelmer.vidaos.ui.historial.HistorialRoute
import com.wuelmer.vidaos.ui.movimientos.MovimientosRoute
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.SelectorModulos
import com.wuelmer.vidaos.ui.navegacion.modulosVisibles
import com.wuelmer.vidaos.ui.registrargasto.RegistrarGastoRoute
import com.wuelmer.vidaos.ui.theme.VidaOSTheme

// Pestañas superiores de cada módulo; la primera de cada uno es su pantalla de inicio.
private enum class Pestana(val modulo: Modulo, val ruta: String, val etiqueta: String) {
    REGISTRAR(Modulo.FINANZAS, "registrar", "Registrar"),
    MOVIMIENTOS(Modulo.FINANZAS, "movimientos", "Movimientos"),
    GYM_PROGRESO(Modulo.GYM, "gym_progreso", "Progreso"),
    GYM_SESIONES(Modulo.GYM, "gym_inicio", "Sesiones");

    companion object {
        fun delModulo(modulo: Modulo): List<Pestana> = entries.filter { it.modulo == modulo }
    }
}

private const val RUTA_HISTORIAL = "historial"
private const val RUTA_CATEGORIAS = "categorias"
private const val ARG_MOVIMIENTO_ID = "movimientoId"
private const val RUTA_DETALLE = "detalle/{$ARG_MOVIMIENTO_ID}"
private const val ARG_DIA_ID = "diaId"
private const val ARG_SESION_ID = "sesionId"
// sesionId opcional: sin él se registra una sesión nueva; con él se edita la guardada.
private const val RUTA_GYM_SESION = "gym_sesion/{$ARG_DIA_ID}?$ARG_SESION_ID={$ARG_SESION_ID}"
private const val SIN_SESION = -1L
private const val RUTA_GYM_DETALLE = "gym_detalle/{$ARG_SESION_ID}"
private const val RUTA_CONFIGURACION = "configuracion"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencias = (application as VidaOSApplication).preferencias
        setContent {
            // Hasta leer las preferencias no se dibuja nada, para no parpadear con el tema o los módulos equivocados.
            val prefs by preferencias.preferencias.collectAsState(initial = null)
            val prefsActuales = prefs ?: return@setContent
            val oscuro = when (prefsActuales.tema) {
                Tema.SISTEMA -> isSystemInDarkTheme()
                Tema.CLARO -> false
                Tema.OSCURO -> true
            }
            // Los íconos de la barra de estado siguen al tema elegido, no al del sistema.
            DisposableEffect(oscuro) {
                val estilo = if (oscuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose {}
            }
            VidaOSTheme(darkTheme = oscuro) {
                val modulos = modulosVisibles(prefsActuales.modulosOcultos)
                // El módulo de inicio es el primero visible; si cambia, se rearma la navegación desde él.
                key(modulos.first()) {
                    VidaOSApp(modulos = modulos)
                }
            }
        }
    }
}

@Composable
private fun VidaOSApp(modulos: List<Modulo>) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinoActual = backStackEntry?.destination
    val rutaActual = destinoActual?.route

    val moduloActual = Modulo.entries.firstOrNull { modulo ->
        destinoActual?.hierarchy?.any { it.route == modulo.ruta } == true
    }
    val pestanaActual = Pestana.entries.firstOrNull { it.ruta == rutaActual }
    val esPantallaPrincipal = pestanaActual != null

    // Si se ocultó el módulo en el que estaba (p. ej. al volver de Configuración), ir al primero visible.
    LaunchedEffect(moduloActual, modulos) {
        if (moduloActual != null && moduloActual !in modulos) navController.irAModulo(modulos.first())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Con un solo módulo visible la barra no aporta nada; en Configuración (fuera de los módulos) confunde.
            if (modulos.size > 1 && rutaActual != RUTA_CONFIGURACION) {
                SelectorModulos(
                    modulos = modulos,
                    moduloActual = moduloActual,
                    onModuloClick = { modulo -> navController.irAModulo(modulo) }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (esPantallaPrincipal) {
                EncabezadoModulo(
                    pestanaActual = pestanaActual,
                    titulo = moduloActual?.etiqueta.orEmpty(),
                    onPestanaClick = { navController.irAPestana(it) },
                    onConfiguracionClick = {
                        navController.navigate(RUTA_CONFIGURACION) { launchSingleTop = true }
                    }
                )
            }
            NavHost(
                navController = navController,
                startDestination = modulos.first().ruta,
                modifier = Modifier.weight(1f)
            ) {
                navigation(
                    startDestination = Pestana.REGISTRAR.ruta,
                    route = Modulo.FINANZAS.ruta
                ) {
                    composable(Pestana.REGISTRAR.ruta) { RegistrarGastoRoute() }
                    composable(Pestana.MOVIMIENTOS.ruta) {
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
                composable(RUTA_CONFIGURACION) {
                    ConfiguracionRoute(onBackClick = { navController.popBackStack() })
                }
                navigation(
                    startDestination = Pestana.GYM_PROGRESO.ruta,
                    route = Modulo.GYM.ruta
                ) {
                    composable(Pestana.GYM_PROGRESO.ruta) {
                        ProgresoGymRoute(onSesionClick = { id -> navController.navigate("gym_detalle/$id") })
                    }
                    composable(Pestana.GYM_SESIONES.ruta) {
                        GymRoute(
                            onIniciarSesion = { diaId -> navController.navigate("gym_sesion/$diaId") },
                            onSesionClick = { id -> navController.navigate("gym_detalle/$id") }
                        )
                    }
                    composable(
                        route = RUTA_GYM_DETALLE,
                        arguments = listOf(navArgument(ARG_SESION_ID) { type = NavType.LongType })
                    ) { entry ->
                        val sesionId = entry.arguments?.getLong(ARG_SESION_ID) ?: 0L
                        DetalleSesionGymRoute(
                            sesionId = sesionId,
                            onBackClick = { navController.popBackStack() },
                            onEditarClick = { diaId -> navController.navigate("gym_sesion/$diaId?$ARG_SESION_ID=$sesionId") }
                        )
                    }
                    composable(
                        route = RUTA_GYM_SESION,
                        arguments = listOf(
                            navArgument(ARG_DIA_ID) { type = NavType.LongType },
                            navArgument(ARG_SESION_ID) {
                                type = NavType.LongType
                                defaultValue = SIN_SESION
                            }
                        )
                    ) { entry ->
                        val diaId = entry.arguments?.getLong(ARG_DIA_ID) ?: 0L
                        val sesionId = entry.arguments?.getLong(ARG_SESION_ID)?.takeIf { it != SIN_SESION }
                        SesionGymRoute(
                            diaId = diaId,
                            sesionId = sesionId,
                            onBackClick = { navController.popBackStack() },
                            onGuardado = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// Fila superior de las pantallas principales: pestañas (Finanzas) o título (resto) y el acceso a Configuración.
@Composable
private fun EncabezadoModulo(
    pestanaActual: Pestana?,
    titulo: String,
    onPestanaClick: (Pestana) -> Unit,
    onConfiguracionClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (pestanaActual != null) {
                    val pestanas = Pestana.delModulo(pestanaActual.modulo)
                    PrimaryTabRow(selectedTabIndex = pestanas.indexOf(pestanaActual)) {
                        pestanas.forEach { pestana ->
                            Tab(
                                selected = pestana == pestanaActual,
                                onClick = { onPestanaClick(pestana) },
                                text = { Text(pestana.etiqueta) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            IconButton(onClick = onConfiguracionClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Configuración")
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

private fun NavHostController.irAPestana(pestana: Pestana) {
    navigate(pestana.ruta) {
        popUpTo(Pestana.delModulo(pestana.modulo).first().ruta) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
