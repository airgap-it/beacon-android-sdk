package it.airgap.beaconsdk.internal

import android.content.Context
import it.airgap.beaconsdk.internal.utils.failWithUninitialized

internal class BeaconApp(context: Context) {
    val applicationContext: Context = context.applicationContext

    companion object {
        const val TAG = "BeaconApp"

        private var _instance: BeaconApp? = null
        val instance: BeaconApp
            get() = _instance ?: failWithUninitialized(TAG)

        fun init(context: Context) {
            _instance = BeaconApp(context)
        }
    }
}