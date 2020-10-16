package it.airgap.beaconsdk.internal.utils

import android.util.Log

internal fun logDebug(tag: String, message: String) {
    Log.d("[Beacon SDK] $tag", message)
}