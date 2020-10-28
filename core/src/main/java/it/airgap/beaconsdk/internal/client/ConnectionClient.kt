package it.airgap.beaconsdk.internal.client

import it.airgap.beaconsdk.internal.data.ConnectionMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.flatTryResult
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ConnectionClient(private val transport: Transport, private val serializer: Serializer) {

    fun subscribe(): Flow<InternalResult<BeaconMessage.Request>> =
        transport.subscribe()
            .map { BeaconMessage.fromInternalResult(it)}

    suspend fun send(message: BeaconMessage.Response): InternalResult<Unit> =
        flatTryResult {
            val serialized = serializer.serialize(message).getOrThrow()
            transport.send(serialized)
        }

    private fun BeaconMessage.Companion.fromInternalResult(
        connectionMessage: InternalResult<ConnectionMessage>
    ): InternalResult<BeaconMessage.Request> = connectionMessage.flatMap { serializer.deserialize(it.content) }
}