package it.airgap.beaconsdk.internal.transport

import it.airgap.beaconsdk.data.beacon.Connection
import it.airgap.beaconsdk.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.logDebug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

internal abstract class Transport {
    abstract val type: Connection.Type

    protected abstract val connectionMessages: Flow<InternalResult<ConnectionTransportMessage>>

    protected abstract suspend fun sendMessage(message: ConnectionTransportMessage): InternalResult<Unit>

    fun subscribe(): Flow<InternalResult<ConnectionTransportMessage>> =
        connectionMessages.onStart { logDebug("$TAG $type", "subscribed") }

    suspend fun send(message: ConnectionTransportMessage): InternalResult<Unit> {
        logDebug("$TAG $type", "sending ${message.content} to ${message.origin.id}")
        return sendMessage(message)
    }

    companion object {
        const val TAG = "Transport"
    }
}