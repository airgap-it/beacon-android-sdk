
import androidx.annotation.IntRange
import it.airgap.beaconsdk.core.data.beacon.*
import it.airgap.beaconsdk.core.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.utils.failWith
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// -- extensions --

internal fun <T> List<T>.takeHalf(): List<T> = take(size / 2)

internal fun <T> MutableSharedFlow<Result<T>>.tryEmitValues(values: List<T>) {
    values.forEach { tryEmit(Result.success(it)) }
}

internal fun <T> MutableSharedFlow<Result<T>>.tryEmitFailures(failures: List<Result<T>>) {
    failures.filter { it.isFailure }.forEach { tryEmit(it) }
}

internal fun <T> Flow<T>.onNth(n: Int, action: suspend (T) -> Unit): Flow<T> {
    var counter = 0
    return onEach { if (++counter == n) action(it) }
}

internal fun List<ByteArray>.containsLike(array: ByteArray): Boolean = any { it.contentEquals(array) }

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

// -- flows --

internal fun connectionMessageFlow(
    replay: Int,
): MutableSharedFlow<Result<ConnectionTransportMessage>> = MutableSharedFlow(replay)

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

internal fun chainBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    identifier: String = "chain",
    payload: ChainBeaconRequest.Payload = object : ChainBeaconRequest.Payload() {},
    origin: Origin = Origin.P2P(senderId),
    version: String = "version"
): ChainBeaconRequest = ChainBeaconRequest(id, senderId, appMetadata, identifier, payload, origin, version)

internal fun permissionBeaconResponse(
    id: String = "id",
    publicKey: String = "publicKey",
    network: Network = Network.Custom(),
    scopes: List<Permission.Scope> = emptyList(),
    threshold: Threshold? = null,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): PermissionBeaconResponse = PermissionBeaconResponse(id, publicKey, network, scopes, threshold, version, requestOrigin)

internal fun chainBeaconResponse(
    id: String = "id",
    identifier: String = "chain",
    payload: ChainBeaconResponse.Payload = object : ChainBeaconResponse.Payload() {},
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): ChainBeaconResponse = ChainBeaconResponse(id, identifier, payload, version, requestOrigin)

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
        chainBeaconResponse(version = version, requestOrigin = requestOrigin),
        acknowledgeBeaconResponse(version = version, requestOrigin = requestOrigin),
    ) + errorBeaconResponses(version = version, requestOrigin = requestOrigin)

internal fun beaconRequests(version: String = "version", origin: Origin = Origin.P2P("senderId")): List<BeaconRequest> =
    listOf(
        permissionBeaconRequest(version = version, origin = origin),
        chainBeaconRequest(version = version, origin = origin),
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
        VersionedBeaconMessage.from(senderId, permissionBeaconRequest(senderId = senderId, version = version)),
        VersionedBeaconMessage.from(senderId, chainBeaconRequest(senderId = senderId, version = version)),
    )

internal fun beaconVersionedResponses(version: String = "version", senderId: String = "senderId"): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.from(senderId, permissionBeaconResponse(version = version)),
        VersionedBeaconMessage.from(senderId, chainBeaconResponse(version = version)),
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
    if (includeDisconnect) add(VersionedBeaconMessage.from(senderId, disconnectBeaconMessage(senderId = senderId, version = version)))
    if (includeError) add(VersionedBeaconMessage.from(senderId, errorBeaconResponse(version = version)))
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
): List<Result<T>> =
    (0 until number).map { Result.failure(error) }
