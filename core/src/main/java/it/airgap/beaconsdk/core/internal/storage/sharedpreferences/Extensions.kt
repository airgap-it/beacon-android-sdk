package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T> SharedPreferences.getSerializable(key: String, default: T): T {
    val encoded = getString(key, null)

    return encoded?.let { Json.decodeFromString(encoded) } ?: default
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SharedPreferences.putString(key: String, value: String?) {
    edit().putString(key, value).apply()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SharedPreferences.putStringSet(key: String, value: Set<String>) {
    edit().putStringSet(key, value).apply()
}

@OptIn(ExperimentalSerializationApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T> SharedPreferences.putSerializable(key: String, value: T) {
    val encoded = Json.encodeToString(value)
    putString(key, encoded)
}