package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.data.beacon.Permission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.storage.Storage

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SharedPreferencesStorage(private val sharedPreferences: SharedPreferences) : Storage {
    override suspend fun getPeers(): List<Peer> =
        sharedPreferences.getSerializable(KEY_PEERS, emptyList())

    override suspend fun setPeers(p2pPeers: List<Peer>) {
        sharedPreferences.putSerializable(KEY_PEERS, p2pPeers)
    }

    override suspend fun getAppMetadata(): List<AppMetadata> =
        sharedPreferences.getSerializable(KEY_APPS_METADATA, emptyList())

    override suspend fun setAppMetadata(appMetadata: List<AppMetadata>) {
        sharedPreferences.putSerializable(KEY_APPS_METADATA, appMetadata)
    }

    override suspend fun getPermissions(): List<Permission> =
        sharedPreferences.getSerializable(KEY_PERMISSIONS, emptyList())

    override suspend fun setPermissions(permissions: List<Permission>) {
        sharedPreferences.putSerializable(KEY_PERMISSIONS, permissions)
    }

    override suspend fun getSdkVersion(): String? =
        sharedPreferences.getString(KEY_SDK_VERSION, null)

    override suspend fun setSdkVersion(sdkVersion: String) {
        sharedPreferences.putString(KEY_SDK_VERSION, sdkVersion)
    }

    override suspend fun getMigrations(): Set<String> =
        sharedPreferences.getStringSet(KEY_MIGRATIONS, emptySet()) ?: emptySet()

    override suspend fun setMigrations(migrations: Set<String>) {
        sharedPreferences.putStringSet(KEY_MIGRATIONS, migrations)
    }

    public companion object {
        private const val KEY_PEERS = "peers"

        private const val KEY_APPS_METADATA = "appsMetadata"
        private const val KEY_PERMISSIONS = "permissions"

        private const val KEY_SDK_VERSION = "sdkVersion"

        private const val KEY_MIGRATIONS = "migrations"

        public fun create(context: Context): SharedPreferencesStorage {
            val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)

            return SharedPreferencesStorage(sharedPreferences)
        }
    }
}