package it.airgap.beaconsdk.core.internal.transport

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.Connection
import it.airgap.beaconsdk.core.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.core.internal.utils.logDebug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class Transport {
    public abstract val type: Connection.Type

    protected abstract val connectionMessages: Flow<Result<ConnectionTransportMessage>>

    protected abstract suspend fun sendMessage(message: ConnectionTransportMessage): Result<Unit>

    public fun subscribe(): Flow<Result<ConnectionTransportMessage>> =
        connectionMessages.onStart { logDebug("$TAG $type", "subscribed") }

    public suspend fun send(message: ConnectionTransportMessage): Result<Unit> {
        logDebug("$TAG $type", "sending ${message.content} to ${message.origin.id}")
        return sendMessage(message)
    }

    public companion object {
        internal const val TAG = "Transport"
    }
}