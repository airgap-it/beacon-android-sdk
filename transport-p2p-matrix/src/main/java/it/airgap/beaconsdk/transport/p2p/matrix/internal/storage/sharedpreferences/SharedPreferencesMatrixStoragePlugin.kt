package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.getSerializable
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.putSerializable
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.putString
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin

internal class SharedPreferencesMatrixStoragePlugin(private val sharedPreferences: SharedPreferences) :
    P2pMatrixStoragePlugin {
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

    companion object {
        private const val KEY_MATRIX_RELAY_SERVER = "matrixRelayServer"
        private const val KEY_MATRIX_CHANNELS = "matrixChannels"
        private const val KEY_MATRIX_SYNC_TOKEN = "matrixSyncToken"
        private const val KEY_MATRIX_ROOM_IDS = "matrixRoomIds"

        fun create(context: Context): SharedPreferencesMatrixStoragePlugin {
            val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)

            return SharedPreferencesMatrixStoragePlugin(sharedPreferences)
        }
    }
}