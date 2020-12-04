package it.airgap.beaconsdk.internal.controller

import it.airgap.beaconsdk.data.beacon.Connection
import it.airgap.beaconsdk.exception.ConnectionException
import it.airgap.beaconsdk.exception.MultipleConnectionException
import it.airgap.beaconsdk.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.internal.message.SerializedConnectionMessage
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

internal class ConnectionController(private val transports: List<Transport>, private val serializer: Serializer) {

    fun subscribe(): Flow<InternalResult<BeaconConnectionMessage>> =
        transports
            .map { it.subscribe() }
            .merge()
            .map { BeaconConnectionMessage.fromInternalResult(it)}

    suspend fun send(message: BeaconConnectionMessage): InternalResult<Unit> =
        flatTryResult {
            val serializedContent = serializer.serialize(message.content).get()
            val serializedMessage = SerializedConnectionMessage(message.origin, serializedContent)

            return transports
                .async { it.send(serializedMessage) }
                .foldIndexed(Success()) { index, acc, next ->
                    acc.concat(next, transports[index].type)
                }
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

    private fun InternalResult<Unit>.concat(other: InternalResult<Unit>, connectionType: Connection.Type): InternalResult<Unit> =
        when {
            this is Success<Unit> && other is Success<Unit> -> Success()
            this is Failure<Unit> && other is Success<Unit> -> Failure(ConnectionException.from(connectionType, error))
            this is Success<Unit> && other is Failure<Unit> -> Failure(ConnectionException.from(connectionType, other.error))
            this is Failure<Unit> && other is Failure<Unit> -> {
                val concat = error.concat(ConnectionException.from(connectionType, other.error))
                concat?.let { Failure(concat) } ?: Failure()
            }
            else -> Failure()
        }

    private fun Throwable.concat(other: Throwable): Throwable? =
        when {
            this is MultipleConnectionException && other is MultipleConnectionException ->
                MultipleConnectionException(errors + other.errors.distinctBy { it.type })

            this is MultipleConnectionException && other is ConnectionException ->
                MultipleConnectionException(errors + other)

            this is ConnectionException && other is ConnectionException ->
                MultipleConnectionException(listOf(this, other))

            this is ConnectionException && other is MultipleConnectionException ->
                MultipleConnectionException(other.errors + this)

            else -> null
        }
}