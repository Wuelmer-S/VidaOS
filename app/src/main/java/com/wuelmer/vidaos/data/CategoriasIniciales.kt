package com.wuelmer.vidaos.data

object CategoriasIniciales {
    val lista = listOf(
        Categoria(nombre = "Comida", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Supermercado", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Bencina", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Transporte", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Arriendo", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Servicios", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Salud", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Entretenimiento", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Otros gastos", tipo = TipoCategoria.GASTO),
        Categoria(nombre = "Sueldo", tipo = TipoCategoria.INGRESO),
        Categoria(nombre = "Otros ingresos", tipo = TipoCategoria.INGRESO)
    )
}
