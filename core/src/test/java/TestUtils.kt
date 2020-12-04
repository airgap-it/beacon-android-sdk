
import androidx.annotation.IntRange
import it.airgap.beaconsdk.data.beacon.*
import it.airgap.beaconsdk.data.tezos.TezosActivateAccountOperation
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.internal.utils.failWith
import it.airgap.beaconsdk.message.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// -- extensions --

internal fun <T> List<T>.takeHalf(): List<T> = take(size / 2)

internal fun <T> MutableSharedFlow<InternalResult<T>>.tryEmitValues(values: List<T>) {
    values.forEach { tryEmit(Success(it)) }
}

internal fun <T> MutableSharedFlow<InternalResult<T>>.tryEmitFailures(failures: List<Failure<T>>) {
    failures.forEach { tryEmit(it) }
}

internal fun <T> Flow<T>.onNth(n: Int, action: suspend (T) -> Unit): Flow<T> {
    var counter = 0
    return onEach { if (++counter == n) action(it) }
}

internal fun JsonObject.Companion.fromValues(values: Map<String, Any?>, includeNulls: Boolean = false): JsonObject {
    val content = (if (includeNulls) values else values.filterValues { it != null })
        .mapValues {
            when (val value = it.value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is JsonElement -> value
                null -> JsonNull
                else -> failWith("Can't create JsonObject from $value, unknown type")
            }
        }

    return JsonObject(content)
}

// -- converters --

internal fun versionedBeaconMessage(
    message: BeaconMessage,
    senderId: String = "senderId",
): VersionedBeaconMessage =
    VersionedBeaconMessage.fromBeaconMessage(senderId, message)

internal fun versionedBeaconMessages(
    messages: List<BeaconMessage>,
    senderId: String = "senderId",
): List<VersionedBeaconMessage> =
    messages.map { VersionedBeaconMessage.fromBeaconMessage(senderId, it) }

// -- flows --

internal fun beaconConnectionMessageFlow(
    replay: Int,
): MutableSharedFlow<InternalResult<BeaconConnectionMessage>> = MutableSharedFlow(replay)

internal fun connectionMessageFlow(
    replay: Int,
): MutableSharedFlow<InternalResult<ConnectionTransportMessage>> = MutableSharedFlow(replay)

// -- factories --

internal fun permissionBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    scopes: List<Permission.Scope> = emptyList(),
    origin: Origin = Origin.P2P(senderId),
    version: String = "version",
): PermissionBeaconRequest = PermissionBeaconRequest(id, senderId, appMetadata, network, scopes, origin, version)

internal fun operationBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    operationDetails: List<TezosOperation> = listOf(TezosActivateAccountOperation("pkh", "secret")),
    sourceAddress: String = "sourceAddress",
    origin: Origin = Origin.P2P(senderId),
    version: String = "version",
): OperationBeaconRequest = OperationBeaconRequest(id, senderId, appMetadata, network, operationDetails, sourceAddress, origin, version)

internal fun signPayloadBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    signingType: SigningType = SigningType.Raw,
    payload: String = "payload",
    sourceAddress: String = "sourceAddress",
    origin: Origin = Origin.P2P(senderId),
    version: String = "version",
): SignPayloadBeaconRequest = SignPayloadBeaconRequest(id, senderId, appMetadata, signingType, payload, sourceAddress, origin, version)

internal fun broadcastBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    signedTransaction: String = "signedTransaction",
    origin: Origin = Origin.P2P(senderId),
    version: String = "version",
): BroadcastBeaconRequest = BroadcastBeaconRequest(id, senderId, appMetadata, network, signedTransaction, origin, version)

internal fun permissionBeaconResponse(
    id: String = "id",
    publicKey: String = "publicKey",
    network: Network = Network.Custom(),
    scopes: List<Permission.Scope> = emptyList(),
    threshold: Threshold? = null,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): PermissionBeaconResponse = PermissionBeaconResponse(id, publicKey, network, scopes, threshold, version, requestOrigin)

internal fun operationBeaconResponse(
    id: String = "id",
    transactionHash: String = "transactionHash",
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): OperationBeaconResponse = OperationBeaconResponse(id, transactionHash, version, requestOrigin)

internal fun signPayloadBeaconResponse(
    id: String = "id",
    signingType: SigningType = SigningType.Raw,
    signature: String = "signature",
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): SignPayloadBeaconResponse = SignPayloadBeaconResponse(id, signingType, signature, version, requestOrigin)

internal fun broadcastBeaconResponse(
    id: String = "id",
    transactionHash: String = "transactionHash",
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): BroadcastBeaconResponse = BroadcastBeaconResponse(id, transactionHash, version, requestOrigin)

internal fun acknowledgeBeaconResponse(
    id: String = "id",
    senderId: String = "senderId",
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): AcknowledgeBeaconResponse =
    AcknowledgeBeaconResponse(id, senderId, version, requestOrigin)

internal fun errorBeaconResponse(
    id: String = "id",
    errorType: BeaconError = BeaconError.Unknown,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): ErrorBeaconResponse = ErrorBeaconResponse(id, errorType, version, requestOrigin)

internal fun disconnectBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
    version: String = "version",
    origin: Origin = Origin.P2P("senderId"),
): DisconnectBeaconMessage = DisconnectBeaconMessage(id, senderId, version, origin)

internal fun errorBeaconResponses(
    id: String = "id",
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId")
): List<ErrorBeaconResponse> =
    listOf(
        errorBeaconResponse(id, BeaconError.BroadcastError, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.NetworkNotSupported, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.NoAddressError, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.NoPrivateKeyFound, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.NotGranted, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.ParametersInvalid, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.TooManyOperations, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.TransactionInvalid, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.SignatureTypeNotSupported, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.Aborted, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.Unknown, version = version, requestOrigin = requestOrigin),
    )

internal fun beaconResponses(version: String = "version", requestOrigin: Origin = Origin.P2P("senderId")): List<BeaconResponse> =
    listOf(
        permissionBeaconResponse(version = version, requestOrigin = requestOrigin),
        operationBeaconResponse(version = version, requestOrigin = requestOrigin),
        signPayloadBeaconResponse(version = version, requestOrigin = requestOrigin),
        broadcastBeaconResponse(version = version, requestOrigin = requestOrigin),
        acknowledgeBeaconResponse(version = version, requestOrigin = requestOrigin),
    ) + errorBeaconResponses(version = version, requestOrigin = requestOrigin)

internal fun beaconRequests(version: String = "version", origin: Origin = Origin.P2P("senderId")): List<BeaconRequest> =
    listOf(
        permissionBeaconRequest(version = version, origin = origin),
        operationBeaconRequest(version = version, origin = origin),
        signPayloadBeaconRequest(version = version, origin = origin),
        broadcastBeaconRequest(version = version, origin = origin),
    )

internal fun beaconMessages(
    version: String = "version",
    origin: Origin = Origin.P2P("senderId"),
    includeRequests: Boolean = true,
    includeResponses: Boolean = true,
    includeDisconnect: Boolean = true,
    includeError: Boolean = true,
): List<BeaconMessage> = mutableListOf<BeaconMessage>().apply {
    if (includeRequests) addAll(beaconRequests(version = version, origin = origin))
    if (includeResponses) addAll(beaconResponses(version = version, requestOrigin = origin))
    if (includeDisconnect) add(disconnectBeaconMessage(version = version, origin = origin))
    if (includeError) add(errorBeaconResponse(version = version, requestOrigin = origin))
}

internal fun beaconVersionedRequests(version: String = "version", senderId: String = "senderId"): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.fromBeaconMessage(senderId, permissionBeaconRequest(senderId = senderId, version = version)),
        VersionedBeaconMessage.fromBeaconMessage(senderId, operationBeaconRequest(senderId = senderId, version = version)),
        VersionedBeaconMessage.fromBeaconMessage(senderId, signPayloadBeaconRequest(senderId = senderId, version = version)),
        VersionedBeaconMessage.fromBeaconMessage(senderId, broadcastBeaconRequest(senderId = senderId, version = version)),
    )

internal fun beaconVersionedResponses(version: String = "version", senderId: String = "senderId"): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.fromBeaconMessage(senderId, permissionBeaconResponse(version = version)),
        VersionedBeaconMessage.fromBeaconMessage(senderId, operationBeaconResponse(version = version)),
        VersionedBeaconMessage.fromBeaconMessage(senderId, signPayloadBeaconResponse(version = version)),
        VersionedBeaconMessage.fromBeaconMessage(senderId, broadcastBeaconResponse(version = version)),
    )

internal fun beaconVersionedMessages(
    version: String = "version",
    senderId: String = "senderId",
    includeRequests: Boolean = true,
    includeResponses: Boolean = true,
    includeDisconnect: Boolean = true,
    includeError: Boolean = true,
): List<VersionedBeaconMessage> = mutableListOf<VersionedBeaconMessage>().apply {
    if (includeRequests) addAll(beaconVersionedRequests(version, senderId))
    if (includeResponses) addAll(beaconVersionedResponses(version, senderId))
    if (includeDisconnect) add(VersionedBeaconMessage.fromBeaconMessage(senderId, disconnectBeaconMessage(senderId = senderId, version = version)))
    if (includeError) add(VersionedBeaconMessage.fromBeaconMessage(senderId, errorBeaconResponse(version = version)))
}

internal fun p2pPeers(
    @IntRange(from = 1) number: Int = 1,
    version: String = "version",
    paired: Boolean = false,
): List<P2pPeer> =
    (0 until number).map {
        P2pPeer("id#$it", "name#$it", "publicKey#$it", "relayServer#$it", version, isPaired = paired)
    }

internal fun appMetadata(@IntRange(from = 1) number: Int = 1): List<AppMetadata> =
    (0 until number).map { AppMetadata("sender#$it", "name#$it") }

internal fun permissions(
    @IntRange(from = 1) number: Int = 1,
    network: Network = Network.Custom(),
    scopes: List<Permission.Scope> = emptyList(),
): List<Permission> =
    appMetadata(number).mapIndexed { index, appMetadata ->
        Permission(
            "accountIdentifier#$index",
            "address#$index",
            network,
            scopes,
            "sender#$index",
            appMetadata,
            "publicKey#$index",
            index.toLong(),
        )
    }

internal fun <T> failures(
    @IntRange(from = 1) number: Int = 1,
    error: Throwable = Exception(),
): List<Failure<T>> =
    (0 until number).map { Failure(error) }