
import androidx.annotation.IntRange
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.blockchain.message.BlockchainMockRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.BlockchainMockResponse
import it.airgap.beaconsdk.core.internal.blockchain.message.PermissionMockRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.PermissionMockResponse
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.ConnectionTransportMessage
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v3.V3BeaconMessage
import it.airgap.beaconsdk.core.internal.utils.encodeToString
import it.airgap.beaconsdk.core.internal.utils.failWith
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

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

internal val DependencyRegistry.versionedBeaconMessageContext: VersionedBeaconMessage.Context
    get() = VersionedBeaconMessage.Context(blockchainRegistry, compat)

internal val DependencyRegistry.v1BeaconMessageContext: V1BeaconMessage.Context
    get() = V1BeaconMessage.Context(compat)

internal val DependencyRegistry.v2BeaconMessageContext: V2BeaconMessage.Context
    get() = V2BeaconMessage.Context(compat)

internal val DependencyRegistry.v3BeaconMessageContext: V3BeaconMessage.Context
    get() = V3BeaconMessage.Context(blockchainRegistry)

// -- flows --

internal fun connectionMessageFlow(
    replay: Int,
): MutableSharedFlow<Result<ConnectionTransportMessage>> = MutableSharedFlow(replay)

// -- factories --

internal fun permissionBeaconRequest(
    type: String = "permission_request",
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: MockAppMetadata = MockAppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Origin = Origin.P2P(senderId),
    version: String = "version",
): PermissionBeaconRequest = PermissionMockRequest(type, id, version, blockchainIdentifier, senderId, origin, appMetadata)

internal fun blockchainBeaconRequest(
    type: String = "beacon_request",
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: MockAppMetadata = MockAppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Origin = Origin.P2P(senderId),
    accountId: String = "accountId",
    version: String = "version"
): BlockchainBeaconRequest = BlockchainMockRequest(type, id, version, blockchainIdentifier, senderId, appMetadata, origin, accountId)

internal fun permissionBeaconResponse(
    type: String = "permission_response",
    id: String = "id",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): PermissionBeaconResponse = PermissionMockResponse(type, id, version, requestOrigin, blockchainIdentifier)

internal fun blockchainBeaconResponse(
    type: String = "beacon_response",
    id: String = "id",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): BlockchainBeaconResponse = BlockchainMockResponse(type, id, version, requestOrigin, blockchainIdentifier)

internal fun acknowledgeBeaconResponse(
    id: String = "id",
    senderId: String = "senderId",
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): AcknowledgeBeaconResponse =
    AcknowledgeBeaconResponse(id, version, requestOrigin, senderId)

internal fun errorBeaconResponse(
    id: String = "id",
    errorType: BeaconError = BeaconError.Unknown,
    description: String? = null,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): ErrorBeaconResponse = ErrorBeaconResponse(id, version, requestOrigin, errorType, description)

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
        errorBeaconResponse(id, BeaconError.Aborted, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, BeaconError.Unknown, version = version, requestOrigin = requestOrigin),
    )

internal fun beaconResponses(version: String = "version", requestOrigin: Origin = Origin.P2P("senderId")): List<BeaconResponse> =
    listOf(
        permissionBeaconResponse(version = version, requestOrigin = requestOrigin),
        blockchainBeaconResponse(version = version, requestOrigin = requestOrigin),
        acknowledgeBeaconResponse(version = version, requestOrigin = requestOrigin),
    ) + errorBeaconResponses(version = version, requestOrigin = requestOrigin)

internal fun beaconRequests(version: String = "version", origin: Origin = Origin.P2P("senderId")): List<BeaconRequest> =
    listOf(
        permissionBeaconRequest(version = version, origin = origin),
        blockchainBeaconRequest(version = version, origin = origin),
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

internal fun beaconVersionedRequests(version: String = "version", senderId: String = "senderId", context: VersionedBeaconMessage.Context): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.from(senderId, permissionBeaconRequest(senderId = senderId, version = version), context),
        VersionedBeaconMessage.from(senderId, blockchainBeaconRequest(senderId = senderId, version = version), context),
    )

internal fun beaconVersionedResponses(version: String = "version", senderId: String = "senderId", context: VersionedBeaconMessage.Context): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.from(senderId, permissionBeaconResponse(version = version), context),
        VersionedBeaconMessage.from(senderId, blockchainBeaconResponse(version = version), context),
    )

internal fun beaconVersionedMessages(
    version: String = "version",
    senderId: String = "senderId",
    context: VersionedBeaconMessage.Context,
    includeRequests: Boolean = true,
    includeResponses: Boolean = true,
    includeDisconnect: Boolean = true,
    includeError: Boolean = true,
): List<VersionedBeaconMessage> = mutableListOf<VersionedBeaconMessage>().apply {
    if (includeRequests) addAll(beaconVersionedRequests(version, senderId, context))
    if (includeResponses) addAll(beaconVersionedResponses(version, senderId, context))
    if (includeDisconnect) add(VersionedBeaconMessage.from(senderId, disconnectBeaconMessage(senderId = senderId, version = version), context))
    if (includeError) add(VersionedBeaconMessage.from(senderId, errorBeaconResponse(version = version), context))
}

internal fun p2pPeers(
    @IntRange(from = 1) number: Int = 1,
    version: String = "version",
    paired: Boolean = false,
): List<P2pPeer> =
    (0 until number).map {
        P2pPeer("id#$it", "name#$it", "publicKey#$it", "relayServer#$it", version, isPaired = paired)
    }

internal fun appMetadata(@IntRange(from = 1) number: Int = 1): List<MockAppMetadata> =
    (0 until number).map { MockAppMetadata("sender#$it", "name#$it") }

internal fun permissions(
    @IntRange(from = 1) number: Int = 1,
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
): List<Permission> =
    (0 until number).map {
        MockPermission(
            blockchainIdentifier,
            "accountIdentifier#$it",
            "sender#$it",
            it.toLong(),
        )
    }

internal fun <T> failures(
    @IntRange(from = 1) number: Int = 1,
    error: Throwable = Exception(),
): List<Result<T>> =
    (0 until number).map { Result.failure(error) }
