package it.airgap.beaconsdk.internal.controller

import it.airgap.beaconsdk.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.internal.message.SerializedConnectionMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.flatTryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ConnectionController(private val transport: Transport, private val serializer: Serializer) {

    fun subscribe(): Flow<InternalResult<BeaconConnectionMessage>> =
        transport.subscribe()
            .map { BeaconConnectionMessage.fromInternalResult(it)}

    suspend fun send(message: VersionedBeaconMessage): InternalResult<Unit> =
        flatTryResult {
            val serialized = serializer.serialize(message).value()
            transport.send(serialized)
        }

    private fun BeaconConnectionMessage.Companion.fromInternalResult(
        connectionMessage: InternalResult<ConnectionTransportMessage>
    ): InternalResult<BeaconConnectionMessage> = connectionMessage.flatMap { message ->
        val content = when (message) {
            is SerializedConnectionMessage -> serializer.deserialize(message.content)
            is BeaconConnectionMessage -> Success(message.content)
        }

        content.map { BeaconConnectionMessage(message.origin, it) }
    }
}