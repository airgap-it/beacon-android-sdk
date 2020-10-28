package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class MatrixSyncRoom {

    @Serializable
    data class Joined(val state: MatrixSyncState? = null, val timeline: MatrixSyncTimeline? = null) : MatrixSyncRoom()

    @Serializable
    data class Invited(@SerialName("invite_state") val state: MatrixSyncState? = null): MatrixSyncRoom()

    @Serializable
    data class Left(val state: MatrixSyncState? = null, val timeline: MatrixSyncTimeline? = null): MatrixSyncRoom()
}