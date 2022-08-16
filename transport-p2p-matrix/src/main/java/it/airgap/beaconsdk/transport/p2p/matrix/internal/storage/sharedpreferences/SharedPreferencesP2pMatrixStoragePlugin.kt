package it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesBaseStorage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.transport.p2p.matrix.data.MatrixRoom
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin

internal class SharedPreferencesP2pMatrixStoragePlugin(
    sharedPreferences: SharedPreferences,
    beaconScope: BeaconScope = BeaconScope.Global,
) : P2pMatrixStoragePlugin, SharedPreferencesBaseStorage(beaconScope, sharedPreferences) {

    override suspend fun getMatrixRelayServer(): String? =
        sharedPreferences.getString(Key.MatrixRelayServer.scoped(), null)

    override suspend fun setMatrixRelayServer(relayServer: String?) {
        sharedPreferences.putString(Key.MatrixRelayServer.scoped(), relayServer)
    }

    override suspend fun getMatrixChannels(): Map<String, String> =
        sharedPreferences.getSerializable(Key.MatrixChannels.scoped(), emptyMap(), beaconScope)

    override suspend fun setMatrixChannels(channels: Map<String, String>) {
        sharedPreferences.putSerializable(Key.MatrixChannels.scoped(), channels, beaconScope)
    }

    override suspend fun getMatrixSyncToken(): String? =
        sharedPreferences.getString(Key.MatrixSyncToken.scoped(), null)

    override suspend fun setMatrixSyncToken(syncToken: String?) {
        sharedPreferences.putString(Key.MatrixSyncToken.scoped(), syncToken)
    }

    override suspend fun getMatrixRooms(): List<MatrixRoom> =
        sharedPreferences.getSerializable(Key.MatrixRoomIds.scoped(), emptyList(), beaconScope)

    override suspend fun setMatrixRooms(rooms: List<MatrixRoom>) {
        sharedPreferences.putSerializable(Key.MatrixRoomIds.scoped(), rooms, beaconScope)
    }

    override fun scoped(beaconScope: BeaconScope): P2pMatrixStoragePlugin =
        if (beaconScope == this.beaconScope) this
        else SharedPreferencesP2pMatrixStoragePlugin(sharedPreferences, beaconScope)

    private enum class Key(override val value: String) : SharedPreferencesBaseStorage.Key {
        MatrixRelayServer("matrixRelayServer"),
        MatrixChannels("matrixChannels"),
        MatrixSyncToken("matrixSyncToken"),
        MatrixRoomIds("matrixRoomIds"),
    }
}

internal fun SharedPreferencesP2pMatrixStoragePlugin(context: Context): SharedPreferencesP2pMatrixStoragePlugin {
    val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)

    return SharedPreferencesP2pMatrixStoragePlugin(sharedPreferences)
}