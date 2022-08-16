package it.airgap.beaconsdk.core.internal.transport

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.message.IncomingConnectionTransportMessage
import it.airgap.beaconsdk.core.internal.message.OutgoingConnectionTransportMessage
import it.airgap.beaconsdk.core.internal.utils.logDebug
import it.airgap.beaconsdk.core.transport.data.PairingMessage
import it.airgap.beaconsdk.core.transport.data.PairingRequest
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Transport {
    public abstract val type: Connection.Type

    protected abstract val incomingConnectionMessages: Flow<Result<IncomingConnectionTransportMessage>>

    public abstract suspend fun pair(): Flow<Result<PairingMessage>>
    public abstract suspend fun pair(request: PairingRequest): Result<PairingResponse>

    public abstract fun supportsPairing(request: PairingRequest): Boolean

    protected abstract suspend fun sendMessage(message: OutgoingConnectionTransportMessage): Result<Unit>

    public fun subscribe(): Flow<Result<IncomingConnectionTransportMessage>> =
        incomingConnectionMessages.onStart { logDebug("$TAG $type", "subscribed") }

    public suspend fun send(message: OutgoingConnectionTransportMessage): Result<Unit> {
        logDebug("$TAG $type", "sending ${message.content} to ${message.destination?.id}")
        return sendMessage(message)
    }

    public companion object {
        internal const val TAG = "Transport"
    }
}