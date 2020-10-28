package it.airgap.beaconsdk.internal.transport.p2p.matrix.network

import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.data.BearerHeader
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.api.room.*
import it.airgap.beaconsdk.internal.transport.p2p.matrix.data.client.MatrixRoom
import it.airgap.beaconsdk.internal.utils.InternalResult
import kotlinx.coroutines.flow.single

internal class MatrixRoomService(private val httpClient: HttpClient) {

    suspend fun createRoom(
        accessToken: String,
        roomConfig: MatrixCreateRoomRequest = MatrixCreateRoomRequest()
    ): InternalResult<MatrixCreateRoomResponse> =
        httpClient.post<MatrixCreateRoomRequest, MatrixCreateRoomResponse>(
            "/createRoom",
            body = roomConfig,
            headers = listOf(BearerHeader(accessToken), ApplicationJson())
        ).single()

    suspend fun inviteToRoom(
        accessToken: String,
        user: String,
        roomId: String
    ): InternalResult<MatrixInviteRoomResponse> =
        httpClient.post<MatrixInviteRoomRequest, MatrixInviteRoomResponse>(
            "/rooms/$roomId/invite",
            body = MatrixInviteRoomRequest(user),
            headers = listOf(BearerHeader(accessToken), ApplicationJson())
        ).single()

    suspend fun joinRoom(
        accessToken: String,
        roomId: String,
    ): InternalResult<MatrixJoinRoomResponse> =
        httpClient.post<MatrixJoinRoomRequest, MatrixJoinRoomResponse>(
            "/rooms/$roomId/join",
            body = MatrixJoinRoomRequest(),
            headers = listOf(BearerHeader(accessToken), ApplicationJson())
        ).single()
}