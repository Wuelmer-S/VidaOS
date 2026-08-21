package com.wuelmer.vidaos.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(fecha: LocalDate?): Long? = fecha?.toEpochDay()

    @TypeConverter
    fun fromOrigenPago(value: OrigenPago?): String? = value?.name

    @TypeConverter
    fun toOrigenPago(value: String?): OrigenPago? = value?.let(OrigenPago::valueOf)

    @TypeConverter
    fun fromTipoMovimiento(value: TipoMovimiento?): String? = value?.name

    @TypeConverter
    fun toTipoMovimiento(value: String?): TipoMovimiento? = value?.let(TipoMovimiento::valueOf)

    @TypeConverter
    fun fromTipoPrestamo(value: TipoPrestamo?): String? = value?.name

    @TypeConverter
    fun toTipoPrestamo(value: String?): TipoPrestamo? = value?.let(TipoPrestamo::valueOf)

    @TypeConverter
    fun fromTipoCategoria(value: TipoCategoria?): String? = value?.name

    @TypeConverter
    fun toTipoCategoria(value: String?): TipoCategoria? = value?.let(TipoCategoria::valueOf)
}
