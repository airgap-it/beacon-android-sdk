package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.store

import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.MatrixRoom

public data class MatrixStoreState(
    val isPolling: Boolean = false,
    val userId: String? = null,
    val deviceId: String? = null,
    val transactionCounter: Int = 0,
    val accessToken: String? = null,
    val syncToken: String? = null,
    val pollingTimeout: Long? = null,
    val pollingRetries: Int = 0,
    val rooms: Map<String, MatrixRoom> = emptyMap(),
)