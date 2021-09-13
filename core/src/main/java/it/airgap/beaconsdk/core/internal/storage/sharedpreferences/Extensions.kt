package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import android.content.SharedPreferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public inline fun <reified T> SharedPreferences.getSerializable(key: String, default: T): T {
    val encoded = getString(key, null)

    return encoded?.let { Json.decodeFromString(encoded) } ?: default
}

public fun SharedPreferences.putString(key: String, value: String?) {
    edit().putString(key, value).apply()
}

public fun SharedPreferences.putStringSet(key: String, value: Set<String>) {
    edit().putStringSet(key, value).apply()
}

public inline fun <reified T> SharedPreferences.putSerializable(key: String, value: T) {
    val encoded = Json.encodeToString(value)
    putString(key, encoded)
}