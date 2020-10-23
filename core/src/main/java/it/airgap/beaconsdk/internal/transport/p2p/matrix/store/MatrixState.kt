package it.airgap.beaconsdk.internal.transport.p2p.matrix.store

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixRoom

internal data class MatrixState(
    val isActive: Boolean = false,
    val userId: String? = null,
    val deviceId: String? = null,
    val transactionCounter: Int = 0,
    val accessToken: String? = null,
    val syncToken: String? = null,
    val pollingTimeout: Int? = null,
    val pollingRetries: Int = 0,
    val rooms: Map<String, MatrixRoom> = emptyMap(),
)