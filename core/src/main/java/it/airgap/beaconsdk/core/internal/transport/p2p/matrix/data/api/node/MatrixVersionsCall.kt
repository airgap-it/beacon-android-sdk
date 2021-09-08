package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.node

import kotlinx.serialization.Serializable

@Serializable
internal data class MatrixVersionsResponse(val versions: List<String>)