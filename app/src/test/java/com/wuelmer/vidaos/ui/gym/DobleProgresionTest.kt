package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.data.ZonaEjercicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DobleProgresionTest {

    private val sentadilla = EjercicioGym(1, "Sentadilla", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR)
    private val pressBanca = EjercicioGym(2, "Press banca", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR)
    private val dominadas = EjercicioGym(3, "Dominadas", TipoEjercicio.PESO_CORPORAL, ZonaEjercicio.SUPERIOR)
    private val plancha = EjercicioGym(4, "Plancha", TipoEjercicio.TIEMPO, ZonaEjercicio.CORE)

    private fun serie(reps: Int? = null, peso: Double? = null, segundos: Int? = null) =
        SerieGym(sesionId = 1, ejercicioId = 1, orden = 1, repeticiones = reps, segundos = segundos, pesoKg = peso)

    @Test
    fun sinHistorial_noSugiereNada() {
        assertNull(sugerir(sentadilla, 3, 8, emptyList()))
    }

    @Test
    fun todasEnTopeMismoPeso_inferior_sube2coma5() {
        val series = List(3) { serie(reps = 8, peso = 60.0) }
        assertEquals(Sugerencia.SubirPeso(62.5), sugerir(sentadilla, 3, 8, series))
    }

    @Test
    fun todasEnTopeMismoPeso_superior_sube1() {
        val series = List(4) { serie(reps = 8, peso = 60.0) }
        assertEquals(Sugerencia.SubirPeso(61.0), sugerir(pressBanca, 4, 8, series))
    }

    @Test
    fun unaSerieBajoTope_mantiene() {
        val series = listOf(serie(8, 60.0), serie(8, 60.0), serie(7, 60.0))
        assertEquals(Sugerencia.MantenerPeso(60.0), sugerir(sentadilla, 3, 8, series))
    }

    @Test
    fun faltanSeries_mantiene() {
        val series = List(2) { serie(reps = 8, peso = 60.0) }
        assertEquals(Sugerencia.MantenerPeso(60.0), sugerir(sentadilla, 3, 8, series))
    }

    @Test
    fun pesosDistintos_mantieneElMayor() {
        val series = listOf(serie(8, 60.0), serie(8, 60.0), serie(8, 57.5))
        assertEquals(Sugerencia.MantenerPeso(60.0), sugerir(sentadilla, 3, 8, series))
    }

    @Test
    fun incrementoSinRestosDeComaFlotante() {
        val series = List(3) { serie(reps = 10, peso = 10.2) }
        assertEquals(Sugerencia.SubirPeso(11.2), sugerir(pressBanca, 3, 10, series))
    }

    @Test
    fun pesoCorporal_enTope_rangoCompletado() {
        val series = List(4) { serie(reps = 10) }
        assertEquals(Sugerencia.RangoCompletado, sugerir(dominadas, 4, 10, series))
    }

    @Test
    fun pesoCorporal_bajoTope_nada() {
        val series = listOf(serie(reps = 10), serie(reps = 9))
        assertNull(sugerir(dominadas, 2, 10, series))
    }

    @Test
    fun tiempo_enTope_rangoCompletado() {
        val series = List(3) { serie(segundos = 45) }
        assertEquals(Sugerencia.RangoCompletado, sugerir(plancha, 3, 45, series))
    }

    @Test
    fun textoUltimaVez_mismoPeso() {
        val series = listOf(serie(8, 62.5), serie(8, 62.5), serie(7, 62.5))
        assertEquals("8 / 8 / 7 reps · 62,5 kg", textoUltimaVez(sentadilla, series))
    }

    @Test
    fun textoUltimaVez_pesosDistintos() {
        val series = listOf(serie(8, 60.0), serie(6, 57.5))
        assertEquals("8×60 / 6×57,5 kg", textoUltimaVez(sentadilla, series))
    }

    @Test
    fun libras_inferior_sube5() {
        // 135 lb guardadas en kg, como lo haría la app al registrar en libras.
        val series = List(3) { serie(reps = 8, peso = UnidadPeso.LB.aKg(135.0)) }
        assertEquals(Sugerencia.SubirPeso(140.0), sugerir(sentadilla, 3, 8, series, UnidadPeso.LB))
    }

    @Test
    fun libras_superior_sube2coma5() {
        val series = List(4) { serie(reps = 8, peso = UnidadPeso.LB.aKg(95.0)) }
        assertEquals(Sugerencia.SubirPeso(97.5), sugerir(pressBanca, 4, 8, series, UnidadPeso.LB))
    }

    @Test
    fun conversion_idaYVuelta_conservaElValor() {
        assertEquals(135.0, UnidadPeso.LB.desdeKg(UnidadPeso.LB.aKg(135.0)), 0.0)
        assertEquals(22.5, UnidadPeso.LB.desdeKg(UnidadPeso.LB.aKg(22.5)), 0.0)
    }

    @Test
    fun textoUltimaVez_enLibras() {
        val series = List(2) { serie(8, UnidadPeso.LB.aKg(135.0)) }
        assertEquals("8 / 8 reps · 135 lb", textoUltimaVez(sentadilla, series, UnidadPeso.LB))
    }
}
