package it.airgap.beaconsdk.internal.utils

import android.util.Log
import java.util.*

private const val GLOBAL_TAG = "[Beacon SDK]"

internal fun logInfo(tag: String, message: String) {
    Log.i("$GLOBAL_TAG $tag", message.capitalize(Locale.ROOT))
}

internal fun logDebug(tag: String, message: String) {
    Log.d("$GLOBAL_TAG $tag", message.capitalize(Locale.ROOT))
}

internal fun logError(tag: String, error: Throwable) {
    Log.e("$GLOBAL_TAG $tag", error.message?.capitalize(Locale.ROOT), error)
}