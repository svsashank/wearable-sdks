package org.smriti.app

import android.app.Application
import com.lm.sdk.LmAPILite
import org.smriti.app.device.DeviceRegistry

class SmritiApp : Application() {
    val registry = DeviceRegistry()

    override fun onCreate() {
        super.onCreate()
        LmAPILite.init(this) // ring SDK; local BLE only. No token/cloud calls anywhere in this app.
    }
}
