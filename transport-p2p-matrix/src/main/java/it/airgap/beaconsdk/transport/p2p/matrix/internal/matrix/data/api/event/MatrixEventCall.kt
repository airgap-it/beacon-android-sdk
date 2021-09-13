package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixEventResponse(@SerialName("event_id") val eventId: String? = null)