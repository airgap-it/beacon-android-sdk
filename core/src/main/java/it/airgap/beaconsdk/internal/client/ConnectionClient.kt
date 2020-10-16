package it.airgap.beaconsdk.internal.client

import it.airgap.beaconsdk.internal.message.ConnectionMessage
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.message.beaconmessage.ApiBeaconMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.suspendFlatTryInternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ConnectionClient(private val transport: Transport, private val serializer: Serializer) {

    fun subscribe(): Flow<InternalResult<ApiBeaconMessage.Request>> =
        transport.subscribe()
            .map { ApiBeaconMessage.fromInternalResult(it)}

    suspend fun send(message: ApiBeaconMessage.Response): InternalResult<Unit> =
        suspendFlatTryInternal {
            val serialized = serializer.serialize(message).getOrThrow()
            transport.send(serialized)
        }

    private fun ApiBeaconMessage.Companion.fromInternalResult(
        connectionMessage: InternalResult<ConnectionMessage>
    ): InternalResult<ApiBeaconMessage.Request> = connectionMessage.flatMap { serializer.deserialize(it.content) }
}