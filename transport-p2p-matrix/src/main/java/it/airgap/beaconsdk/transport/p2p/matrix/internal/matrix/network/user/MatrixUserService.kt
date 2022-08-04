package it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.user

import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.network.HttpClient.Companion.post
import it.airgap.beaconsdk.core.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.MatrixError
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.login.MatrixLoginRequest
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.data.api.login.MatrixLoginResponse
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.MatrixService
import kotlinx.coroutines.flow.single

internal class MatrixUserService(httpClient: HttpClient) : MatrixService(httpClient) {

    suspend fun login(
        node: String,
        user: String,
        password: String,
        deviceId: String,
    ): Result<MatrixLoginResponse> =
        withApi(node) { baseUrl ->
            httpClient.post<MatrixLoginRequest, MatrixLoginResponse, MatrixError>(
                baseUrl,
                "/login",
                body = MatrixLoginRequest.Password(
                    MatrixLoginRequest.UserIdentifier.User(user),
                    password,
                    deviceId
                ),
                headers = listOf(ApplicationJson()),
            ).single()
        }
}