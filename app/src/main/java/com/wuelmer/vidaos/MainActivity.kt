package com.wuelmer.vidaos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
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
import com.wuelmer.vidaos.avisos.Notificador
import com.wuelmer.vidaos.data.Tema
import com.wuelmer.vidaos.ui.categorias.CategoriasRoute
import com.wuelmer.vidaos.ui.configuracion.ConfiguracionRoute
import com.wuelmer.vidaos.ui.detalle.DetalleMovimientoRoute
import com.wuelmer.vidaos.ui.gym.DetalleSesionGymRoute
import com.wuelmer.vidaos.ui.gym.EvolucionEjercicioRoute
import com.wuelmer.vidaos.ui.gym.GymRoute
import com.wuelmer.vidaos.ui.gym.ProgresoGymRoute
import com.wuelmer.vidaos.ui.gym.SesionGymRoute
import com.wuelmer.vidaos.ui.historial.HistorialRoute
import com.wuelmer.vidaos.ui.moto.DetalleMantencionRoute
import com.wuelmer.vidaos.ui.moto.FichaRoute
import com.wuelmer.vidaos.ui.moto.FichasRoute
import com.wuelmer.vidaos.ui.moto.LecturasKmRoute
import com.wuelmer.vidaos.ui.moto.MotoRoute
import com.wuelmer.vidaos.ui.movimientos.MovimientosRoute
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.SelectorModulos
import com.wuelmer.vidaos.ui.navegacion.modulosVisibles
import com.wuelmer.vidaos.ui.registrargasto.RegistrarGastoRoute
import com.wuelmer.vidaos.ui.tarjeta.FacturaRoute
import com.wuelmer.vidaos.ui.tarjeta.ImportarTarjetaRoute
import com.wuelmer.vidaos.ui.tarjeta.TarjetaRoute
import com.wuelmer.vidaos.ui.theme.VidaOSTheme

// Pestañas superiores de cada módulo; la primera de cada uno es su pantalla de inicio.
private enum class Pestana(val modulo: Modulo, val ruta: String, val etiqueta: String) {
    REGISTRAR(Modulo.FINANZAS, "registrar", "Registrar"),
    MOVIMIENTOS(Modulo.FINANZAS, "movimientos", "Movimientos"),
    TARJETA(Modulo.FINANZAS, "tarjeta", "Tarjeta"),
    GYM_PROGRESO(Modulo.GYM, "gym_progreso", "Progreso"),
    GYM_SESIONES(Modulo.GYM, "gym_inicio", "Sesiones"),
    MOTO_PLAN(Modulo.MOTO, "moto_inicio", "Plan"),
    MOTO_FICHAS(Modulo.MOTO, "moto_fichas", "Fichas");

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
private const val ARG_EJERCICIO_ID = "ejercicioId"
private const val RUTA_GYM_EJERCICIO = "gym_ejercicio/{$ARG_EJERCICIO_ID}"
private const val RUTA_CONFIGURACION = "configuracion"
private const val ARG_TIPO_ID = "tipoId"
private const val RUTA_MOTO_MANTENCION = "moto_mantencion/{$ARG_TIPO_ID}"
private const val RUTA_MOTO_KM = "moto_km"
private const val ARG_TRABAJO_ID = "trabajoId"
private const val RUTA_MOTO_FICHA = "moto_ficha/{$ARG_TRABAJO_ID}"
private const val ARG_URI = "uri"
private const val RUTA_TARJETA_IMPORTAR = "tarjeta_importar?$ARG_URI={$ARG_URI}"
private const val ARG_ESTADO_ID = "estadoId"
private const val RUTA_TARJETA_FACTURA = "tarjeta_factura/{$ARG_ESTADO_ID}"

private fun rutaImportar(uri: Uri): String = "tarjeta_importar?$ARG_URI=${Uri.encode(uri.toString())}"

// Pantalla que pidió abrir una notificación (p. ej. el detalle de una mantención) o un PDF compartido desde otra app.
private data class DestinoNotificacion(val modulo: Modulo, val ruta: String?)

private fun destinoDe(intent: Intent?): DestinoNotificacion? {
    // "Compartir → vidaOS" con el PDF del estado de cuenta.
    if (intent?.action == Intent.ACTION_SEND && intent.type == "application/pdf") {
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return null
        return DestinoNotificacion(Modulo.FINANZAS, rutaImportar(uri))
    }
    val modulo = intent?.getStringExtra(Notificador.EXTRA_MODULO)
        ?.let { nombre -> Modulo.entries.firstOrNull { it.name == nombre } } ?: return null
    return DestinoNotificacion(modulo, intent.getStringExtra(Notificador.EXTRA_RUTA))
}

class MainActivity : ComponentActivity() {
    private val destinoNotificacion = mutableStateOf<DestinoNotificacion?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        destinoNotificacion.value = destinoDe(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al recrearse (p. ej. girar la pantalla) no se vuelve a abrir la pantalla de la notificación.
        if (savedInstanceState == null) destinoNotificacion.value = destinoDe(intent)
        enableEdgeToEdge()
        val preferencias = (application as VidaOSApplication).preferencias
        setContent {
            PedirPermisoNotificaciones()
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
                    VidaOSApp(
                        modulos = modulos,
                        destino = destinoNotificacion.value,
                        onDestinoUsado = { destinoNotificacion.value = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun VidaOSApp(
    modulos: List<Modulo>,
    destino: DestinoNotificacion?,
    onDestinoUsado: () -> Unit
) {
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

    LaunchedEffect(destino) {
        if (destino == null) return@LaunchedEffect
        if (destino.modulo in modulos) {
            navController.irAModulo(destino.modulo)
            destino.ruta?.let { navController.navigate(it) }
        }
        onDestinoUsado()
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
                    composable(Pestana.TARJETA.ruta) {
                        TarjetaRoute(
                            onImportar = { uri -> navController.navigate(rutaImportar(uri)) },
                            onFacturaClick = { id -> navController.navigate("tarjeta_factura/$id") }
                        )
                    }
                    composable(
                        route = RUTA_TARJETA_IMPORTAR,
                        arguments = listOf(navArgument(ARG_URI) { type = NavType.StringType })
                    ) { entry ->
                        val uri = Uri.parse(entry.arguments?.getString(ARG_URI).orEmpty())
                        ImportarTarjetaRoute(
                            uri = uri,
                            onBackClick = { navController.popBackStack() },
                            onGuardada = { navController.irAPestana(Pestana.TARJETA) }
                        )
                    }
                    composable(
                        route = RUTA_TARJETA_FACTURA,
                        arguments = listOf(navArgument(ARG_ESTADO_ID) { type = NavType.LongType })
                    ) { entry ->
                        FacturaRoute(
                            estadoId = entry.arguments?.getLong(ARG_ESTADO_ID) ?: 0L,
                            onBackClick = { navController.popBackStack() }
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
                        ProgresoGymRoute(
                            onSesionClick = { id -> navController.navigate("gym_detalle/$id") },
                            onEjercicioClick = { id -> navController.navigate("gym_ejercicio/$id") }
                        )
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
                            onEditarClick = { diaId -> navController.navigate("gym_sesion/$diaId?$ARG_SESION_ID=$sesionId") },
                            onEjercicioClick = { id -> navController.navigate("gym_ejercicio/$id") }
                        )
                    }
                    composable(
                        route = RUTA_GYM_EJERCICIO,
                        arguments = listOf(navArgument(ARG_EJERCICIO_ID) { type = NavType.LongType })
                    ) { entry ->
                        EvolucionEjercicioRoute(
                            ejercicioId = entry.arguments?.getLong(ARG_EJERCICIO_ID) ?: 0L,
                            onBackClick = { navController.popBackStack() },
                            onSesionClick = { id -> navController.navigate("gym_detalle/$id") }
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
                navigation(
                    startDestination = Pestana.MOTO_PLAN.ruta,
                    route = Modulo.MOTO.ruta
                ) {
                    composable(Pestana.MOTO_PLAN.ruta) {
                        MotoRoute(
                            onMantencionClick = { id -> navController.navigate("moto_mantencion/$id") },
                            onVerLecturasClick = { navController.navigate(RUTA_MOTO_KM) }
                        )
                    }
                    composable(
                        route = RUTA_MOTO_MANTENCION,
                        arguments = listOf(navArgument(ARG_TIPO_ID) { type = NavType.LongType })
                    ) { entry ->
                        DetalleMantencionRoute(
                            tipoId = entry.arguments?.getLong(ARG_TIPO_ID) ?: 0L,
                            onBackClick = { navController.popBackStack() },
                            onVerFicha = { id -> navController.navigate("moto_ficha/$id") }
                        )
                    }
                    composable(Pestana.MOTO_FICHAS.ruta) {
                        FichasRoute(onFichaClick = { id -> navController.navigate("moto_ficha/$id") })
                    }
                    composable(
                        route = RUTA_MOTO_FICHA,
                        arguments = listOf(navArgument(ARG_TRABAJO_ID) { type = NavType.LongType })
                    ) { entry ->
                        FichaRoute(
                            trabajoId = entry.arguments?.getLong(ARG_TRABAJO_ID) ?: 0L,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(RUTA_MOTO_KM) {
                        LecturasKmRoute(onBackClick = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

// Android 13+ pide permiso para notificar. Se pregunta al abrir la app; si lo niega, Configuración lo recuerda.
@Composable
private fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val permiso = Manifest.permission.POST_NOTIFICATIONS
        if (context.checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) launcher.launch(permiso)
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
                                text = { Text(pestana.etiqueta, maxLines = 1, softWrap = false) }
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

// Sin saveState/restoreState: anidado con el de irAModulo dejaba trabada la barra de módulos
// (p. ej. Sesiones → Progreso → Sesiones → Finanzas no navegaba). La primera pestaña nunca se
// saca de la pila y las demás se reconstruyen desde la base de datos, así que no se pierde nada.
private fun NavHostController.irAPestana(pestana: Pestana) {
    navigate(pestana.ruta) {
        popUpTo(Pestana.delModulo(pestana.modulo).first().ruta)
        launchSingleTop = true
    }
}
