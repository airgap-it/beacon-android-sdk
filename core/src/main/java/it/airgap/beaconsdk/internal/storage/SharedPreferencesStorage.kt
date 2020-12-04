package it.airgap.beaconsdk.internal.storage

import android.content.Context
import android.content.SharedPreferences
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Peer
import it.airgap.beaconsdk.data.beacon.Permission
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.MatrixRoom
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    override suspend fun getMatrixSyncToken(): String? =
        sharedPreferences.getString(KEY_MATRIX_SYNC_TOKEN, null)

    override suspend fun setMatrixSyncToken(syncToken: String) {
        sharedPreferences.putString(KEY_MATRIX_SYNC_TOKEN, syncToken)
    }

    override suspend fun getMatrixRooms(): List<MatrixRoom> =
        sharedPreferences.getSerializable(KEY_MATRIX_ROOM_IDS, emptyList())

    override suspend fun setMatrixRooms(rooms: List<MatrixRoom>) {
        sharedPreferences.putSerializable(KEY_MATRIX_ROOM_IDS, rooms)
    }

    override suspend fun getSdkSecretSeed(): String? =
        sharedPreferences.getString(KEY_SDK_SECRET_SEED, null)

    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) {
        sharedPreferences.putString(KEY_SDK_SECRET_SEED, sdkSecretSeed)
    }

    override suspend fun getSdkVersion(): String? =
        sharedPreferences.getString(KEY_SDK_VERSION, null)

    override suspend fun setSdkVersion(sdkVersion: String) {
        sharedPreferences.putString(KEY_SDK_VERSION, sdkVersion)
    }

    private inline fun <reified T> SharedPreferences.getSerializable(key: String, default: T): T {
        val encoded = getString(key, null)

        return encoded?.let { Json.decodeFromString(encoded) } ?: default
    }

    private fun SharedPreferences.putString(key: String, value: String) {
        edit().putString(key, value).apply()
    }

    private inline fun <reified T> SharedPreferences.putSerializable(key: String, value: T) {
        val encoded = Json.encodeToString(value)
        putString(key, encoded)
    }

    companion object {
        private const val STORAGE_NAME = "beaconsdk"

        const val KEY_PEERS = "peers"

        const val KEY_APPS_METADATA = "appsMetadata"
        const val KEY_PERMISSIONS = "permissions"

        const val KEY_MATRIX_SYNC_TOKEN = "matrixSyncToken"
        const val KEY_MATRIX_ROOM_IDS = "matrixRoomIds"

        const val KEY_SDK_SECRET_SEED = "sdkSecretSeed"
        const val KEY_SDK_VERSION = "sdkVersion"

        fun create(context: Context): SharedPreferencesStorage {
            val sharedPreferences = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)

            return SharedPreferencesStorage(sharedPreferences)
        }
    }
}