package it.airgap.beaconsdk.core.internal.transport

import it.airgap.beaconsdk.core.data.beacon.Connection
import it.airgap.beaconsdk.core.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.core.internal.utils.logDebug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

public abstract class Transport {
    abstract val type: Connection.Type

    protected abstract val connectionMessages: Flow<Result<ConnectionTransportMessage>>

    protected abstract suspend fun sendMessage(message: ConnectionTransportMessage): Result<Unit>

    fun subscribe(): Flow<Result<ConnectionTransportMessage>> =
        connectionMessages.onStart { logDebug("$TAG $type", "subscribed") }

    suspend fun send(message: ConnectionTransportMessage): Result<Unit> {
        logDebug("$TAG $type", "sending ${message.content} to ${message.origin.id}")
        return sendMessage(message)
    }

    companion object {
        const val TAG = "Transport"
    }
}