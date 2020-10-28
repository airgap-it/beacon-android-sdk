package it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.sync

import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.event.MatrixStateEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
internal data class MatrixSyncState(val events: List<MatrixStateEvent<@Contextual Any>>? = null)