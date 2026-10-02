package com.wuelmer.vidaos.ui.navegacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuloTest {

    @Test
    fun sinOcultos_todosVisibles() {
        assertEquals(Modulo.entries, modulosVisibles(emptySet()))
    }

    @Test
    fun ocultarUno_loQuita() {
        assertEquals(listOf(Modulo.GYM, Modulo.MOTO), modulosVisibles(setOf(Modulo.FINANZAS.name)))
    }

    @Test
    fun todosOcultos_seMuestranTodos() {
        assertEquals(Modulo.entries, modulosVisibles(Modulo.entries.map { it.name }.toSet()))
    }

    @Test
    fun nombresDesconocidos_seIgnoran() {
        assertEquals(Modulo.entries, modulosVisibles(setOf("MOTO_VIEJO")))
    }

    @Test
    fun conVariosVisibles_sePuedeOcultar() {
        assertTrue(puedeOcultar(Modulo.GYM, emptySet()))
    }

    @Test
    fun elUltimoVisible_noSePuedeOcultar() {
        assertFalse(puedeOcultar(Modulo.GYM, setOf(Modulo.FINANZAS.name, Modulo.MOTO.name)))
    }
}
