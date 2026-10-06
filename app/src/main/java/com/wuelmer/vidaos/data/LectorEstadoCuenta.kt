package com.wuelmer.vidaos.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Lo que se leyó de un PDF, antes de guardarlo. estado.id y operaciones.estadoId van en 0.
data class FacturaLeida(
    val estado: EstadoCuenta,
    val operaciones: List<OperacionTarjeta>,
    // Facturado anterior + todas las líneas: debe dar el total que informa el banco.
    val sumaCalculada: Long
) {
    val cuadra: Boolean get() = sumaCalculada == estado.totalFacturado
}

sealed interface ResultadoLectura {
    data class Ok(val factura: FacturaLeida) : ResultadoLectura
    data class Error(val mensaje: String) : ResultadoLectura
}

// Lee el estado de cuenta NACIONAL de tarjeta de crédito de Banco de Chile / Edwards a partir del texto
// que entrega PdfBox (PDFTextStripper con sortByPosition = true). La parte internacional se ignora:
// su deuda ya viene incluida en la nacional como "TRASPASO DEUDA INTERNACIONAL".
object LectorEstadoCuenta {

    private val fecha4 = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val fecha2 = DateTimeFormatter.ofPattern("dd/MM/yy")
    private const val F = """(\d{2}/\d{2}/\d{4})"""
    private const val M = """\$\s*(-?[\d.]+)"""

    // [lugar] fecha código descripción $ monto $ total NN/NN $ cuota
    private val lineaOperacion = Regex(
        """^\s*(?:.*?\s)?(\d{2}/\d{2}/\d{2})\s+\d{12}\s+(.+?)\s+$M\s+$M\s+(\d{2})/(\d{2})\s+$M\s*$"""
    )

    fun leer(textoCompleto: String, hoy: LocalDate = LocalDate.now()): ResultadoLectura {
        if (!textoCompleto.contains("ESTADO DE CUENTA NACIONAL DE TARJETA DE CR")) {
            return ResultadoLectura.Error("No parece un estado de cuenta de tarjeta de crédito de Banco de Chile / Edwards.")
        }
        val texto = textoCompleto.substringBefore("ESTADO DE CUENTA INTERNACIONAL")

        val fechaEstado = buscarFecha(texto, """FECHA ESTADO DE CUENTA\s+$F""")
        val periodo = Regex("""PER[IÍ]ODO FACTURADO\s+$F\s+$F""").find(texto)
        val pagarHasta = buscarFecha(texto, """PAGAR HASTA\s+$F""")
        val total = buscarMonto(texto, """MONTO TOTAL FACTURADO A PAGAR\s*\(.*?\)\s+$M""")
        val minimo = buscarMonto(texto, """MONTO M[IÍ]NIMO A PAGAR\s+$M""")
        val cupo = Regex("""(?m)^\s*CUPO TOTAL\s+$M\s+$M\s+$M""").find(texto)
        val anterior = buscarMonto(texto, """MONTO FACTURADO A PAGAR \(PER[IÍ]ODO ANTERIOR\)\s+A\s+$M""")
        val proximo = Regex("""PR[OÓ]XIMO PER[IÍ]ODO DE FACTURACI[OÓ]N\s+$F\s+$F""").find(texto)

        if (fechaEstado == null || periodo == null || pagarHasta == null || total == null || minimo == null || cupo == null) {
            return ResultadoLectura.Error("No se encontraron los datos principales (fechas, total o cupo). ¿El banco cambió el formato?")
        }

        val operaciones = leerOperaciones(texto)
        val estado = EstadoCuenta(
            fechaEstado = fechaEstado,
            periodoDesde = LocalDate.parse(periodo.groupValues[1], fecha4),
            periodoHasta = LocalDate.parse(periodo.groupValues[2], fecha4),
            pagarHasta = pagarHasta,
            totalFacturado = total,
            montoMinimo = minimo,
            cupoTotal = monto(cupo.groupValues[1]),
            cupoUtilizado = monto(cupo.groupValues[2]),
            cupoDisponible = monto(cupo.groupValues[3]),
            facturadoAnterior = anterior ?: 0,
            proximoDesde = proximo?.let { LocalDate.parse(it.groupValues[1], fecha4) },
            proximoHasta = proximo?.let { LocalDate.parse(it.groupValues[2], fecha4) },
            vencimientos = leerVencimientos(texto).joinToString(","),
            importadoEl = hoy
        )
        val suma = estado.facturadoAnterior + operaciones.sumOf { it.valorCuota }
        return ResultadoLectura.Ok(FacturaLeida(estado, operaciones, suma))
    }

    // La sección define el tipo: "1.TOTAL OPERACIONES" trae pagos y compras; "3.CARGOS..." los cargos del banco.
    private fun leerOperaciones(texto: String): List<OperacionTarjeta> {
        val operaciones = mutableListOf<OperacionTarjeta>()
        var enCargos = false
        texto.lines().forEach { linea ->
            when {
                linea.contains("TOTAL OPERACIONES") -> enCargos = false
                linea.contains("PRODUCTOS O SERVICIOS VOLUNTARIAMENTE") -> enCargos = true
                linea.contains("CARGOS, COMISIONES, IMPUESTOS") -> enCargos = true
            }
            val m = lineaOperacion.find(linea) ?: return@forEach
            val (fecha, descripcion, montoOp, _, actual, totalCuotas, cuota) = m.destructured
            val montoOperacion = monto(montoOp)
            val cuotasTotal = totalCuotas.toInt()
            val tipo = when {
                enCargos -> TipoOperacionTarjeta.CARGO
                montoOperacion < 0 || descripcion.contains("MONTO CANCELADO") -> TipoOperacionTarjeta.PAGO
                cuotasTotal > 1 -> TipoOperacionTarjeta.CUOTA
                else -> TipoOperacionTarjeta.COMPRA
            }
            operaciones += OperacionTarjeta(
                estadoId = 0,
                fecha = LocalDate.parse(fecha, fecha2),
                descripcion = limpiarDescripcion(descripcion),
                tipo = tipo,
                montoOperacion = montoOperacion,
                cuotaActual = actual.toInt(),
                cuotasTotal = cuotasTotal,
                valorCuota = monto(cuota),
                orden = operaciones.size
            )
        }
        return operaciones
    }

    // "VENCIMIENTO PRÓXIMOS 4 MESES": la línea con los montos trae ACTUAL + 4 meses. Se guardan los 4 meses.
    private fun leerVencimientos(texto: String): List<Long> {
        val lineas = texto.lines()
        val inicio = lineas.indexOfFirst { it.contains("VENCIMIENTO PR") }
        if (inicio < 0) return emptyList()
        val montos = lineas.drop(inicio + 1).take(4)
            .map { linea -> Regex(M).findAll(linea).map { monto(it.groupValues[1]) }.toList() }
            .firstOrNull { it.size >= 5 } ?: return emptyList()
        return montos.subList(1, 5)
    }

    // "COPEC APP              SANTIAGO" → "COPEC APP SANTIAGO"; quita la tasa de las compras en cuotas.
    private fun limpiarDescripcion(descripcion: String): String =
        descripcion.replace(Regex("""\s+TASA INT\..*$"""), "").replace(Regex("""\s+"""), " ").trim()

    private fun buscarFecha(texto: String, patron: String): LocalDate? =
        Regex(patron).find(texto)?.let { LocalDate.parse(it.groupValues[1], fecha4) }

    private fun buscarMonto(texto: String, patron: String): Long? =
        Regex(patron).find(texto)?.let { monto(it.groupValues[1]) }

    // "-333.476" → -333476
    private fun monto(texto: String): Long = texto.replace(".", "").toLong()
}
