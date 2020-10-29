package it.airgap.beaconsdk.internal.controller

import it.airgap.beaconsdk.internal.message.SerializedBeaconMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.flatTryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ConnectionController(private val transport: Transport, private val serializer: Serializer) {

    fun subscribe(): Flow<InternalResult<VersionedBeaconMessage>> =
        transport.subscribe()
            .map { VersionedBeaconMessage.fromInternalResult(it)}

    suspend fun send(message: VersionedBeaconMessage): InternalResult<Unit> =
        flatTryResult {
            val serialized = serializer.serialize(message).value()
            transport.send(serialized)
        }

    private fun VersionedBeaconMessage.Companion.fromInternalResult(
        connectionMessage: InternalResult<SerializedBeaconMessage>
    ): InternalResult<VersionedBeaconMessage> = connectionMessage.flatMap { serializer.deserialize(it.content) }
}