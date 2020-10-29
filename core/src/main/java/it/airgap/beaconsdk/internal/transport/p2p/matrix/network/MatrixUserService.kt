package it.airgap.beaconsdk.internal.transport.p2p.matrix.network

import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.login.MatrixLoginRequest
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.login.MatrixLoginResponse
import it.airgap.beaconsdk.internal.utils.InternalResult
import kotlinx.coroutines.flow.single

internal class MatrixUserService(private val httpClient: HttpClient) {

    suspend fun login(
        user: String,
        password: String,
        deviceId: String,
    ): InternalResult<MatrixLoginResponse> =
        httpClient.post<MatrixLoginRequest, MatrixLoginResponse>(
            "/login",
            body = MatrixLoginRequest.Password(
                MatrixLoginRequest.UserIdentifier.User(user),
                password,
                deviceId
            ),
            headers = listOf(ApplicationJson())
        ).single()

}