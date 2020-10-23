package it.airgap.beaconsdk.internal.transport.p2p.matrix.store

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync.MatrixSyncResponse

internal sealed class MatrixStoreAction {
    data class Init(
        val userId: String,
        val deviceId: String,
        val accessToken: String,
    ) : MatrixStoreAction()

    data class OnSyncSuccess(
        val syncToken: String?,
        val pollingTimeout: Int,
        val syncRooms: MatrixSyncResponse.Rooms?,
    ) : MatrixStoreAction()

    object OnSyncError : MatrixStoreAction()

    object OnTxnIdCreated : MatrixStoreAction()
}