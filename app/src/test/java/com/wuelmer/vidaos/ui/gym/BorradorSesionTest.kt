package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.BorradorSerie
import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.data.ZonaEjercicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BorradorSesionTest {

    private val dominadas = EjercicioGym(9, "Dominadas", TipoEjercicio.PESO_CORPORAL, ZonaEjercicio.SUPERIOR)
    private val jalon = EjercicioGym(10, "Jalón al pecho", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR)
    private val pesoMuerto = EjercicioGym(8, "Peso muerto rumano", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR)

    private fun ejercicio(id: Long, vararg opciones: EjercicioGym, series: Int = 3) = EjercicioSesionUi(
        rutinaEjercicioId = id,
        opciones = opciones.toList(),
        elegidoId = opciones.first().id,
        series = series,
        objetivoMin = 6,
        objetivoMax = 8,
        descansoSegundos = 150,
        inputs = List(series) { SerieInput() }
    )

    private val iniciales = listOf(ejercicio(1, pesoMuerto), ejercicio(2, dominadas, jalon))

    @Test
    fun sinNadaEscrito_noHayBorrador() {
        assertTrue(aBorrador(5, iniciales).isEmpty())
    }

    @Test
    fun conDatos_guardaTodasLasFilasYVuelveIgual() {
        val editados = listOf(
            iniciales[0].copy(inputs = listOf(SerieInput("8", "60"), SerieInput("7", "60,5"), SerieInput())),
            iniciales[1]
        )
        val borrador = aBorrador(5, editados)
        assertEquals(6, borrador.size)
        assertTrue(borrador.all { it.diaRutinaId == 5L })

        val recuperados = aplicarBorrador(iniciales, borrador)
        assertEquals(listOf("8", "7", ""), recuperados[0].inputs.map { it.reps })
        assertEquals(listOf("60", "60,5", ""), recuperados[0].inputs.map { it.peso })
        assertEquals(3, recuperados[1].inputs.size)
    }

    @Test
    fun soloElegirAlternativa_seGuarda() {
        val editados = listOf(iniciales[0], iniciales[1].copy(elegidoId = jalon.id))
        val recuperados = aplicarBorrador(iniciales, aBorrador(5, editados))
        assertEquals(jalon.id, recuperados[1].elegidoId)
    }

    @Test
    fun seriesExtra_seConservan() {
        val editados = listOf(iniciales[0].copy(inputs = List(5) { SerieInput() }), iniciales[1])
        val recuperados = aplicarBorrador(iniciales, aBorrador(5, editados))
        assertEquals(5, recuperados[0].inputs.size)
    }

    @Test
    fun ejercicioQueYaNoEsOpcion_mantieneElOriginal() {
        val borrador = listOf(BorradorSerie(0, 5, 2, ejercicioId = 999, orden = 0, reps = "8", peso = "", segundos = ""))
        val recuperados = aplicarBorrador(iniciales, borrador)
        assertEquals(dominadas.id, recuperados[1].elegidoId)
        assertEquals("8", recuperados[1].inputs.single().reps)
    }

    @Test
    fun ordenDesordenado_seRespeta() {
        val borrador = listOf(
            BorradorSerie(0, 5, 1, pesoMuerto.id, orden = 1, reps = "7", peso = "60", segundos = ""),
            BorradorSerie(0, 5, 1, pesoMuerto.id, orden = 0, reps = "8", peso = "60", segundos = "")
        )
        assertEquals(listOf("8", "7"), aplicarBorrador(iniciales, borrador)[0].inputs.map { it.reps })
    }

    // --- Editar una sesión guardada ---

    private fun guardada(ejercicioId: Long, orden: Int, reps: Int?, pesoKg: Double? = null) =
        SerieGym(id = orden.toLong(), sesionId = 1, ejercicioId = ejercicioId, orden = orden, repeticiones = reps, pesoKg = pesoKg)

    @Test
    fun editar_cargaLasSeriesEnSuEjercicio() {
        val series = listOf(guardada(pesoMuerto.id, 1, 8, 60.0), guardada(pesoMuerto.id, 2, 7, 62.5), guardada(dominadas.id, 3, 10))
        val (ejercicios, ajenas) = aplicarSeriesGuardadas(iniciales, series, UnidadPeso.KG)
        assertEquals(listOf("8", "7"), ejercicios[0].inputs.map { it.reps })
        assertEquals(listOf("60", "62,5"), ejercicios[0].inputs.map { it.peso })
        assertEquals(listOf("10"), ejercicios[1].inputs.map { it.reps })
        assertTrue(ajenas.isEmpty())
    }

    @Test
    fun editar_respetaLaAlternativaUsada() {
        val series = listOf(guardada(jalon.id, 1, 10, 45.0))
        val (ejercicios, _) = aplicarSeriesGuardadas(iniciales, series, UnidadPeso.KG)
        assertEquals(jalon.id, ejercicios[1].elegidoId)
        assertEquals(3, ejercicios[0].inputs.size) // sin series guardadas: filas vacías del objetivo
    }

    @Test
    fun editar_enLibras_muestraElPesoConvertido() {
        val series = listOf(guardada(pesoMuerto.id, 1, 8, UnidadPeso.LB.aKg(135.0)))
        val (ejercicios, _) = aplicarSeriesGuardadas(iniciales, series, UnidadPeso.LB)
        assertEquals("135", ejercicios[0].inputs.single().peso)
    }

    @Test
    fun editar_seriesDeOtroEjercicio_seConservanAparte() {
        val series = listOf(guardada(pesoMuerto.id, 1, 8, 60.0), guardada(999, 2, 12, 20.0))
        val (_, ajenas) = aplicarSeriesGuardadas(iniciales, series, UnidadPeso.KG)
        assertEquals(listOf(999L), ajenas.map { it.ejercicioId })
    }
}
