package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class MatrixSyncRoom {

    @Serializable
    data class Joined(val state: MatrixSyncState?, val timeline: MatrixSyncTimeline?) : MatrixSyncRoom()

    @Serializable
    data class Invited(@SerialName("invite_state") val state: MatrixSyncState?): MatrixSyncRoom()

    @Serializable
    data class Left(val state: MatrixSyncState?, val timeline: MatrixSyncTimeline?): MatrixSyncRoom()
}