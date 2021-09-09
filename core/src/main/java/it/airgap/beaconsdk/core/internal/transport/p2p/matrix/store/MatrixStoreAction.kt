package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.store

import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixEvent
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixRoom

public sealed interface MatrixStoreAction

public data class Init(
    val userId: String,
    val deviceId: String,
    val accessToken: String,
) : MatrixStoreAction

public data class OnSyncSuccess(
    val syncToken: String?,
    val pollingTimeout: Long,
    val rooms: List<MatrixRoom>?,
    val events: List<MatrixEvent>?,
) : MatrixStoreAction

public object OnSyncError : MatrixStoreAction

public object OnTxnIdCreated : MatrixStoreAction

public object Reset : MatrixStoreAction
public object HardReset : MatrixStoreAction