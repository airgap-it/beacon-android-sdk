package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Maybe
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.exception.BlockchainNotFoundException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.utils.json
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage
import kotlinx.serialization.Contextual
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T> SharedPreferences.getSerializable(key: String, default: @Contextual T, beaconScope: BeaconScope): T {
    val encoded = getString(key, null)

    return encoded?.let { json(beaconScope).decodeFromString(encoded) } ?: default
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SharedPreferences.putString(key: String, value: String?) {
    edit().putString(key, value).apply()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun SharedPreferences.putStringSet(key: String, value: Set<String>) {
    edit().putStringSet(key, value).apply()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T> SharedPreferences.putSerializable(key: String, value: @Contextual T, beaconScope: BeaconScope) {
    val encoded = json(beaconScope).encodeToString(value)
    putString(key, encoded)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun Storage.getPeers(beaconConfiguration: BeaconConfiguration): List<Peer> = getMaybePeers().values(beaconConfiguration)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun Storage.getAppMetadata(beaconConfiguration: BeaconConfiguration): List<AppMetadata> = getMaybeAppMetadata().values(beaconConfiguration)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun Storage.getPermissions(beaconConfiguration: BeaconConfiguration): List<Permission> = getMaybePermissions().values(beaconConfiguration)

private fun <T> List<Maybe<T>>.values(beaconConfiguration: BeaconConfiguration): List<T> =
    mapNotNull {
        when (it) {
            is Maybe.Some -> it.value
            is Maybe.None -> null
            is Maybe.NoneWithError -> it.discardOrThrow(beaconConfiguration)
        }
    }

private fun <T> Maybe.NoneWithError<T>.discardOrThrow(beaconConfiguration: BeaconConfiguration): T? = with (beaconConfiguration) {
    when {
        exception is BlockchainNotFoundException && ignoreUnsupportedBlockchains -> null
        else -> throw exception
    }
}