package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.network.node

import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.MatrixError
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.data.api.node.MatrixVersionsResponse
import it.airgap.beaconsdk.core.internal.transport.p2p.matrix.network.MatrixService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single

internal class MatrixNodeService(httpClient: HttpClient) : MatrixService(httpClient) {

    suspend fun isUp(node: String): Boolean =
        withApiBase(node) { baseUrl ->
            httpClient.get<MatrixVersionsResponse, MatrixError>(baseUrl, "/versions")
                .map { it.isSuccess }
                .single()
        }
}