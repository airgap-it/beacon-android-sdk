
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.MockAppMetadata
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.blockchain.message.BlockchainMockRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.BlockchainMockResponse
import it.airgap.beaconsdk.core.internal.blockchain.message.PermissionMockRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.PermissionMockResponse
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.BeaconIncomingConnectionMessage
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.flow.MutableSharedFlow

// -- extensions --

internal fun <T> MutableSharedFlow<Result<T>>.tryEmitValues(values: List<T>) {
    values.forEach { tryEmit(Result.success(it)) }
}

internal val DependencyRegistry.versionedBeaconMessageContext: VersionedBeaconMessage.Context
    get() = VersionedBeaconMessage.Context(blockchainRegistry, compat)

// -- converters --

internal fun versionedBeaconMessage(
    message: BeaconMessage,
    senderId: String = "senderId",
    context: VersionedBeaconMessage.Context,
): VersionedBeaconMessage =
    VersionedBeaconMessage.from(senderId, message, context)

// -- flows --

internal fun beaconConnectionMessageFlow(
    replay: Int,
): MutableSharedFlow<Result<BeaconIncomingConnectionMessage>> = MutableSharedFlow(replay)

// -- factories --

internal fun permissionBeaconRequest(
    type: String = "permission_request",
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: MockAppMetadata = MockAppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Connection.Id = Connection.Id.P2P(senderId),
    destination: Connection.Id? = Connection.Id.P2P("receiverId"),
    version: String = "version",
): PermissionBeaconRequest = PermissionMockRequest(type, id, version, blockchainIdentifier, senderId, origin, destination, appMetadata)

internal fun blockchainBeaconRequest(
    type: String = "beacon_request",
    id: String = "id",
    senderId: String = "senderId",
    accountId: String? = "accountId",
    appMetadata: MockAppMetadata = MockAppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Connection.Id = Connection.Id.P2P(senderId),
    destination: Connection.Id? = Connection.Id.P2P("receiverId"),
    version: String = "version"
): BlockchainBeaconRequest = BlockchainMockRequest(type, id, version, blockchainIdentifier, senderId, appMetadata, origin, destination, accountId)

internal fun permissionBeaconResponse(
    type: String = "permission_response",
    id: String = "id",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    destination: Connection.Id = Connection.Id.P2P("receiverId"),
): PermissionBeaconResponse = PermissionMockResponse(type, id, version, destination, blockchainIdentifier)

internal fun blockchainBeaconResponse(
    type: String = "beacon_response",
    id: String = "id",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    destination: Connection.Id = Connection.Id.P2P("receiverId"),
): BlockchainBeaconResponse = BlockchainMockResponse(type, id, version, destination, blockchainIdentifier)

internal fun acknowledgeBeaconResponse(
    id: String = "id",
    senderId: String = "senderId",
    version: String = "version",
    destination: Connection.Id = Connection.Id.P2P("receiverId"),
): AcknowledgeBeaconResponse =
    AcknowledgeBeaconResponse(id, version, destination, senderId)

internal fun errorBeaconResponse(
    id: String = "id",
    errorType: BeaconError = BeaconError.Unknown,
    description: String? = null,
    version: String = "version",
    destination: Connection.Id = Connection.Id.P2P("receiverId"),
): ErrorBeaconResponse = ErrorBeaconResponse(id, version, destination, errorType, description)

internal fun errorBeaconResponses(
    id: String = "id",
    version: String = "version",
    destination: Connection.Id = Connection.Id.P2P("receiverId"),
): List<ErrorBeaconResponse> =
    listOf(
        errorBeaconResponse(id, BeaconError.Aborted, version = version, destination = destination),
        errorBeaconResponse(id, BeaconError.Unknown, version = version, destination = destination),
    )

internal fun beaconResponses(version: String = "version", requestOrigin: Connection.Id = Connection.Id.P2P("senderId")): List<BeaconResponse> =
    listOf(
        permissionBeaconResponse(version = version, destination = requestOrigin),
        blockchainBeaconResponse(version = version, destination = requestOrigin),
        acknowledgeBeaconResponse(version = version, destination = requestOrigin),
    ) + errorBeaconResponses(version = version, destination = requestOrigin)

internal fun beaconVersionedRequests(version: String = "version", senderId: String = "senderId", context: VersionedBeaconMessage.Context): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.from(senderId, permissionBeaconRequest(senderId = senderId, version = version), context),
        VersionedBeaconMessage.from(senderId, blockchainBeaconRequest(senderId = senderId, version = version), context),
    )
