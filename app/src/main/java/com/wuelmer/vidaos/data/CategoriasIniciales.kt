package com.wuelmer.vidaos.data

object CategoriasIniciales {

    // Colores de la paleta de categorías (registro/Ideas/paleta_colores_app.html), en rotación.
    private val colores = listOf("#7B61FF", "#ED4C8B", "#37D5D6", "#FF7A59", "#FFA26B")

    val lista = listOf(
        Categoria(nombre = "Comida", tipo = TipoCategoria.GASTO, color = colores[0]),
        Categoria(nombre = "Supermercado", tipo = TipoCategoria.GASTO, color = colores[1]),
        Categoria(nombre = "Bencina", tipo = TipoCategoria.GASTO, color = colores[2]),
        Categoria(nombre = "Transporte", tipo = TipoCategoria.GASTO, color = colores[3]),
        Categoria(nombre = "Arriendo", tipo = TipoCategoria.GASTO, color = colores[4]),
        Categoria(nombre = "Servicios", tipo = TipoCategoria.GASTO, color = colores[0]),
        Categoria(nombre = "Salud", tipo = TipoCategoria.GASTO, color = colores[1]),
        Categoria(nombre = "Entretenimiento", tipo = TipoCategoria.GASTO, color = colores[2]),
        Categoria(nombre = "Otros gastos", tipo = TipoCategoria.GASTO, color = colores[3]),
        Categoria(nombre = "Sueldo", tipo = TipoCategoria.INGRESO, color = colores[4]),
        Categoria(nombre = "Otros ingresos", tipo = TipoCategoria.INGRESO, color = colores[0])
    )
}
