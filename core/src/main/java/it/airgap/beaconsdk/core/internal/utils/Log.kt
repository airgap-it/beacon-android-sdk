package it.airgap.beaconsdk.core.internal.utils

import android.util.Log
import androidx.annotation.RestrictTo

private const val GLOBAL_TAG = "[Beacon SDK]"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun logInfo(tag: String, message: String) {
    Log.i("$GLOBAL_TAG $tag", message.capitalized())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun logDebug(tag: String, message: String) {
    Log.d("$GLOBAL_TAG $tag", message.capitalized())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun logError(tag: String, error: Throwable) {
    Log.e("$GLOBAL_TAG $tag", error.message?.capitalized(), error)
}