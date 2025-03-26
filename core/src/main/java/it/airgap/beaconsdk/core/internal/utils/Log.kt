package it.airgap.beaconsdk.core.internal.utils

import android.util.Log
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.configuration.LogLevel
import it.airgap.beaconsdk.core.internal.BeaconConfiguration

private const val GLOBAL_TAG = "[Beacon SDK]"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Logger(private val tag: String, private val configuration: BeaconConfiguration) {
    public fun info(message: String) {
        logInfo(tag, message, configuration)
    }

    public fun debug(message: String) {
        logDebug(tag, message, configuration)
    }

    public fun error(error: Throwable) {
        logError(tag, error, configuration)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun logInfo(tag: String, message: String, configuration: BeaconConfiguration? = null) {
    if (configuration != null && configuration.logLevel < LogLevel.Info) return

    Log.i("$GLOBAL_TAG $tag", message.capitalized())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun logDebug(tag: String, message: String, configuration: BeaconConfiguration) {
    if (configuration.logLevel < LogLevel.Debug) return

    Log.d("$GLOBAL_TAG $tag", message.capitalized())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun logError(tag: String, error: Throwable, configuration: BeaconConfiguration) {
    if (configuration.logLevel < LogLevel.Error) return

    Log.e("$GLOBAL_TAG $tag", error.message?.capitalized(), error)
}