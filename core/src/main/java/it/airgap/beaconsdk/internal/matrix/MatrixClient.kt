package it.airgap.beaconsdk.internal.matrix

import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.matrix.data.client.MatrixClientEvent
import kotlinx.coroutines.flow.Flow

// TODO: types
internal interface MatrixClient {
    val joinedRooms: List<Any>
    val invitedRooms: List<Any>
    val leftRooms: List<Any>

    val events: Flow<InternalResult<MatrixClientEvent<*>>>

    suspend fun start(id: String, password: String, deviceId: String)

    suspend fun createTrustedPrivateRoom(vararg members: String)

    suspend fun inviteToRooms(user: String, vararg roomIds: String)
    suspend fun inviteToRooms(user: String, vararg rooms: Any)

    suspend fun joinRooms(user: String, vararg roomIds: String)
    suspend fun joinRooms(user: String, vararg rooms: Any)

    suspend fun sendTextMessage(roomId: String, message: String)
    suspend fun sendTextMessage(room: Any, message: String)
}