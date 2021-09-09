package it.airgap.beaconsdk.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.storage.Storage
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom

internal class SharedPreferencesStorage(private val sharedPreferences: SharedPreferences) : Storage {
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

    override suspend fun getMatrixRelayServer(): String? =
        sharedPreferences.getString(KEY_MATRIX_RELAY_SERVER, null)

    override suspend fun setMatrixRelayServer(relayServer: String?) {
        sharedPreferences.putString(KEY_MATRIX_RELAY_SERVER, relayServer)
    }

    override suspend fun getMatrixChannels(): Map<String, String> =
        sharedPreferences.getSerializable(KEY_MATRIX_CHANNELS, emptyMap())

    override suspend fun setMatrixChannels(channels: Map<String, String>) {
        sharedPreferences.putSerializable(KEY_MATRIX_CHANNELS, channels)
    }

    override suspend fun getMatrixSyncToken(): String? =
        sharedPreferences.getString(KEY_MATRIX_SYNC_TOKEN, null)

    override suspend fun setMatrixSyncToken(syncToken: String?) {
        sharedPreferences.putString(KEY_MATRIX_SYNC_TOKEN, syncToken)
    }

    override suspend fun getMatrixRooms(): List<MatrixRoom> =
        sharedPreferences.getSerializable(KEY_MATRIX_ROOM_IDS, emptyList())

    override suspend fun setMatrixRooms(rooms: List<MatrixRoom>) {
        sharedPreferences.putSerializable(KEY_MATRIX_ROOM_IDS, rooms)
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

    companion object {
        private const val KEY_PEERS = "peers"

        private const val KEY_APPS_METADATA = "appsMetadata"
        private const val KEY_PERMISSIONS = "permissions"

        private const val KEY_MATRIX_RELAY_SERVER = "matrixRelayServer"
        private const val KEY_MATRIX_CHANNELS = "matrixChannels"
        private const val KEY_MATRIX_SYNC_TOKEN = "matrixSyncToken"
        private const val KEY_MATRIX_ROOM_IDS = "matrixRoomIds"

        private const val KEY_SDK_VERSION = "sdkVersion"

        private const val KEY_MIGRATIONS = "migrations"

        fun create(context: Context): SharedPreferencesStorage {
            val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)

            return SharedPreferencesStorage(sharedPreferences)
        }
    }
}