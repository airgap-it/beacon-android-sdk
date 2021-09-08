package it.airgap.beaconsdk.core.internal.transport.p2p.matrix.network

import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.network.HttpClient

internal abstract class MatrixService(protected val httpClient: HttpClient) {

    protected inline fun <T> withApiBase(node: String, action: (baseUrl: String) -> T): T = action(apiUrl(node))
    protected inline fun <T> withApi(node: String, action: (baseUrl: String) -> T): T =
        withApiBase(node) { action("$it/${BeaconConfiguration.MATRIX_CLIENT_API_VERSION.trimStart('/')}") }

    private fun apiUrl(node: String): String = "https://$node/${BeaconConfiguration.MATRIX_CLIENT_API_BASE.trimStart('/')}"
}