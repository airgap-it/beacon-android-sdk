
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
    version: String = "version",
    senderId: String = "senderId",
): VersionedBeaconMessage =
    VersionedBeaconMessage.fromBeaconMessage(version, senderId, message)

internal fun versionedBeaconMessages(
    messages: List<BeaconMessage>,
    version: String = "version",
    senderId: String = "senderId",
): List<VersionedBeaconMessage> =
    messages.map { VersionedBeaconMessage.fromBeaconMessage(version, senderId, it) }

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
    scopes: List<PermissionScope> = emptyList(),
    origin: Origin = Origin.P2P(senderId),
): PermissionBeaconRequest = PermissionBeaconRequest(id, senderId, appMetadata, network, scopes, origin)

internal fun operationBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    operationDetails: List<TezosOperation> = listOf(TezosActivateAccountOperation("pkh", "secret")),
    sourceAddress: String = "sourceAddress",
    origin: Origin = Origin.P2P(senderId),
): OperationBeaconRequest = OperationBeaconRequest(id, senderId, appMetadata, network, operationDetails, sourceAddress, origin)

internal fun signPayloadBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    payload: String = "payload",
    sourceAddress: String = "sourceAddress",
    origin: Origin = Origin.P2P(senderId),
): SignPayloadBeaconRequest = SignPayloadBeaconRequest(id, senderId, appMetadata, payload, sourceAddress, origin)

internal fun broadcastBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    signedTransaction: String = "signedTransaction",
    origin: Origin = Origin.P2P(senderId),
): BroadcastBeaconRequest = BroadcastBeaconRequest(id, senderId, appMetadata, network, signedTransaction, origin)

internal fun permissionBeaconResponse(
    id: String = "id",
    publicKey: String = "publicKey",
    network: Network = Network.Custom(),
    scopes: List<PermissionScope> = emptyList(),
): PermissionBeaconResponse = PermissionBeaconResponse(id, publicKey, network, scopes)

internal fun operationBeaconResponse(
    id: String = "id",
    transactionHash: String = "transactionHash",
): OperationBeaconResponse = OperationBeaconResponse(id, transactionHash)

internal fun signPayloadBeaconResponse(
    id: String = "id",
    signature: String = "signature",
): SignPayloadBeaconResponse = SignPayloadBeaconResponse(id, signature)

internal fun broadcastBeaconResponse(
    id: String = "id",
    transactionHash: String = "transactionHash",
): BroadcastBeaconResponse = BroadcastBeaconResponse(id, transactionHash)

internal fun errorBeaconResponse(
    id: String = "id",
    errorType: BeaconError = BeaconError.Unknown,
): ErrorBeaconResponse = ErrorBeaconResponse(id, errorType)

internal fun disconnectBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
): DisconnectBeaconMessage = DisconnectBeaconMessage(id, senderId)

internal fun errorBeaconResponses(id: String = "id"): List<ErrorBeaconResponse> =
    listOf(
        errorBeaconResponse(id, BeaconError.BroadcastError),
        errorBeaconResponse(id, BeaconError.NetworkNotSupported),
        errorBeaconResponse(id, BeaconError.NoAddressError),
        errorBeaconResponse(id, BeaconError.NoPrivateKeyFound),
        errorBeaconResponse(id, BeaconError.NotGranted),
        errorBeaconResponse(id, BeaconError.ParametersInvalid),
        errorBeaconResponse(id, BeaconError.TooManyOperations),
        errorBeaconResponse(id, BeaconError.TransactionInvalid),
        errorBeaconResponse(id, BeaconError.Aborted),
        errorBeaconResponse(id, BeaconError.Unknown),
    )

internal fun beaconResponses(): List<BeaconResponse> =
    listOf(
        permissionBeaconResponse(),
        operationBeaconResponse(),
        signPayloadBeaconResponse(),
        broadcastBeaconResponse(),
    ) + errorBeaconResponses()

internal fun beaconRequests(): List<BeaconRequest> =
    listOf(
        permissionBeaconRequest(),
        operationBeaconRequest(),
        signPayloadBeaconRequest(),
        broadcastBeaconRequest(),
    )

internal fun beaconMessages(
    includeRequests: Boolean = true,
    includeResponses: Boolean = true,
    includeDisconnect: Boolean = true,
    includeError: Boolean = true,
): List<BeaconMessage> = mutableListOf<BeaconMessage>().apply {
    if (includeRequests) addAll(beaconRequests())
    if (includeResponses) addAll(beaconResponses())
    if (includeDisconnect) add(disconnectBeaconMessage())
    if (includeError) add(errorBeaconResponse())
}

internal fun beaconVersionedRequests(version: String = "version", senderId: String = "senderId"): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, operationBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, signPayloadBeaconRequest(senderId = senderId)),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, broadcastBeaconRequest(senderId = senderId,)),
    )

internal fun beaconVersionedResponses(version: String = "version", senderId: String = "senderId"): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, permissionBeaconResponse()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, operationBeaconResponse()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, signPayloadBeaconResponse()),
        VersionedBeaconMessage.fromBeaconMessage(version, senderId, broadcastBeaconResponse()),
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
    if (includeDisconnect) add(VersionedBeaconMessage.fromBeaconMessage(version, senderId, disconnectBeaconMessage(senderId = senderId)))
    if (includeError) add(VersionedBeaconMessage.fromBeaconMessage(version, senderId, errorBeaconResponse()))
}

internal fun p2pPeers(
    @IntRange(from = 1) number: Int = 1,
    version: String? = null,
    paired: Boolean = false,
): List<P2pPeerInfo> =
    (0 until number).map {
        P2pPeerInfo("name#$it", "publicKey#$it", "relayServer#$it", version, isPaired = paired)
    }

internal fun appMetadata(@IntRange(from = 1) number: Int = 1): List<AppMetadata> =
    (0 until number).map { AppMetadata("sender#$it", "name#$it") }

internal fun permissions(
    @IntRange(from = 1) number: Int = 1,
    network: Network = Network.Custom(),
    scopes: List<PermissionScope> = emptyList(),
): List<PermissionInfo> =
    appMetadata(number).mapIndexed { index, appMetadata ->
        PermissionInfo(
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