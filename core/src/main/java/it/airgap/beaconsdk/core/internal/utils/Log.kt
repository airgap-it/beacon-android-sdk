package it.airgap.beaconsdk.core.internal.utils

import android.util.Log

private const val GLOBAL_TAG = "[Beacon SDK]"

internal fun logInfo(tag: String, message: String) {
    Log.i("$GLOBAL_TAG $tag", message.capitalized())
}

internal fun logDebug(tag: String, message: String) {
    Log.d("$GLOBAL_TAG $tag", message.capitalized())
}

internal fun logError(tag: String, error: Throwable) {
    Log.e("$GLOBAL_TAG $tag", error.message?.capitalized(), error)
}