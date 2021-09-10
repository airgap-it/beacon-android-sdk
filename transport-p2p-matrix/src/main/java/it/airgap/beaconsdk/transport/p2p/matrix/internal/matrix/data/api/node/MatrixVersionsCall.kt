package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.node

import kotlinx.serialization.Serializable

@Serializable
public data class MatrixVersionsResponse(val versions: List<String>)