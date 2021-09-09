package it.airgap.beaconsdk.internal.transport.p2p.matrix.network.room

import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.data.BearerHeader
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.MatrixError
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.*
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixService
import kotlinx.coroutines.flow.single

internal class MatrixRoomService(httpClient: HttpClient) : MatrixService(httpClient) {

    suspend fun createRoom(
        node: String,
        accessToken: String,
        roomConfig: MatrixCreateRoomRequest = MatrixCreateRoomRequest(),
    ): Result<MatrixCreateRoomResponse> =
        withApi(node) { baseUrl ->
            httpClient.post<MatrixCreateRoomRequest, MatrixCreateRoomResponse, MatrixError>(
                baseUrl,
                "/createRoom",
                body = roomConfig,
                headers = listOf(BearerHeader(accessToken), ApplicationJson()),
            ).single()
        }

    suspend fun inviteToRoom(
        node: String,
        accessToken: String,
        user: String,
        roomId: String,
    ): Result<MatrixInviteRoomResponse> =
        withApi(node) { baseUrl ->
            httpClient.post<MatrixInviteRoomRequest, MatrixInviteRoomResponse, MatrixError>(
                baseUrl,
                "/rooms/$roomId/invite",
                body = MatrixInviteRoomRequest(user),
                headers = listOf(BearerHeader(accessToken), ApplicationJson()),
            ).single()
        }

    suspend fun joinRoom(
        node: String,
        accessToken: String,
        roomId: String,
    ): Result<MatrixJoinRoomResponse> =
        withApi(node) { baseUrl ->
            httpClient.post<MatrixJoinRoomRequest, MatrixJoinRoomResponse, MatrixError>(
                baseUrl,
                "/rooms/$roomId/join",
                body = MatrixJoinRoomRequest(),
                headers = listOf(BearerHeader(accessToken), ApplicationJson()),
            ).single()
        }
}