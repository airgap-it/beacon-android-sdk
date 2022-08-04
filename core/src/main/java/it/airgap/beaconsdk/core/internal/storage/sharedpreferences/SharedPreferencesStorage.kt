package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.data.Maybe
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SharedPreferencesStorage internal constructor(
    private val sharedPreferences: SharedPreferences,
    beaconScope: BeaconScope = BeaconScope.Global,
) : Storage, SharedPreferencesBaseStorage(beaconScope) {
    override suspend fun getMaybePeers(): List<Maybe<Peer>> =
        sharedPreferences.getSerializable(Key.Peers.scoped(), emptyList(), beaconScope)

    override suspend fun setPeers(p2pPeers: List<Peer>) {
        sharedPreferences.putSerializable(Key.Peers.scoped(), p2pPeers, beaconScope)
    }

    override suspend fun getMaybeAppMetadata(): List<Maybe<AppMetadata>> =
        sharedPreferences.getSerializable(Key.AppsMetadata.scoped(), emptyList(), beaconScope)

    override suspend fun setAppMetadata(appMetadata: List<AppMetadata>) {
        sharedPreferences.putSerializable(Key.AppsMetadata.scoped(), appMetadata, beaconScope)
    }

    override suspend fun getMaybePermissions(): List<Maybe<Permission>> =
        sharedPreferences.getSerializable(Key.Permissions.scoped(), emptyList(), beaconScope)

    override suspend fun setPermissions(permissions: List<Permission>) {
        sharedPreferences.putSerializable(Key.Permissions.scoped(), permissions, beaconScope)
    }

    override suspend fun getSdkVersion(): String? =
        sharedPreferences.getString(Key.SdkVersion.scoped(), null)

    override suspend fun setSdkVersion(sdkVersion: String) {
        sharedPreferences.putString(Key.SdkVersion.scoped(), sdkVersion)
    }

    override suspend fun getMigrations(): Set<String> =
        sharedPreferences.getStringSet(Key.Migrations.scoped(), emptySet()) ?: emptySet()

    override suspend fun setMigrations(migrations: Set<String>) {
        sharedPreferences.putStringSet(Key.Migrations.scoped(), migrations)
    }

    override fun scoped(beaconScope: BeaconScope): Storage =
        if (beaconScope == this.beaconScope) this
        else SharedPreferencesStorage(sharedPreferences, beaconScope)

    private enum class Key(override val value: String) : SharedPreferencesBaseStorage.Key {
        Peers("peers"),
        AppsMetadata("appsMetadata"),
        Permissions("permissions"),
        SdkVersion("sdkVersion"),
        Migrations("migrations"),
    }

    public companion object {}
}

@Suppress("FunctionName")
internal fun SharedPreferencesStorage(context: Context): SharedPreferencesStorage {
    val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)

    return SharedPreferencesStorage(sharedPreferences)
}