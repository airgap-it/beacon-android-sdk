package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node

import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.network.HttpClient.Companion.get
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.MatrixError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.node.MatrixVersionsResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.MatrixService
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