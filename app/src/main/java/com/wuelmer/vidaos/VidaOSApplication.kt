package com.wuelmer.vidaos

import android.app.Application
import com.wuelmer.vidaos.data.VidaOSDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class VidaOSApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    val database: VidaOSDatabase by lazy { VidaOSDatabase.getInstance(this, applicationScope) }
}
