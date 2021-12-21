
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.blockchain.message.BlockchainMockRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.BlockchainMockResponse
import it.airgap.beaconsdk.core.internal.blockchain.message.PermissionMockRequest
import it.airgap.beaconsdk.core.internal.blockchain.message.PermissionMockResponse
import it.airgap.beaconsdk.core.internal.message.BeaconConnectionMessage
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.message.*
import kotlinx.coroutines.flow.MutableSharedFlow

// -- extensions --

internal fun <T> MutableSharedFlow<Result<T>>.tryEmitValues(values: List<T>) {
    values.forEach { tryEmit(Result.success(it)) }
}

// -- converters --

internal fun versionedBeaconMessage(
    message: BeaconMessage,
    senderId: String = "senderId",
): VersionedBeaconMessage =
    VersionedBeaconMessage.from(senderId, message)

// -- flows --

internal fun beaconConnectionMessageFlow(
    replay: Int,
): MutableSharedFlow<Result<BeaconConnectionMessage>> = MutableSharedFlow(replay)

// -- factories --

internal fun permissionBeaconRequest(
    type: String = "permission_request",
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Origin = Origin.P2P(senderId),
    version: String = "version",
): PermissionBeaconRequest = PermissionMockRequest(type, id, version, blockchainIdentifier, senderId, origin, appMetadata)

internal fun blockchainBeaconRequest(
    type: String = "beacon_request",
    id: String = "id",
    senderId: String = "senderId",
    accountId: String? = "accountId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Origin = Origin.P2P(senderId),
    version: String = "version"
): BlockchainBeaconRequest = BlockchainMockRequest(type, id, version, blockchainIdentifier, senderId, appMetadata, origin, accountId)

internal fun permissionBeaconResponse(
    type: String = "permission_response",
    id: String = "id",
    publicKey: String = "publicKey",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): PermissionBeaconResponse = PermissionMockResponse(type, id, version, requestOrigin, blockchainIdentifier, publicKey)

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
    identifier: String = MockBlockchain.IDENTIFIER,
    errorType: BeaconError = BeaconError.Unknown,
    description: String? = null,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): ErrorBeaconResponse = ErrorBeaconResponse(id, version, requestOrigin, errorType, description, identifier)

internal fun errorBeaconResponses(
    id: String = "id",
    version: String = "version",
    identifier: String = MockBlockchain.IDENTIFIER,
    requestOrigin: Origin = Origin.P2P("senderId")
): List<ErrorBeaconResponse> =
    listOf(
        errorBeaconResponse(id, identifier, BeaconError.NetworkNotSupported, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, identifier, BeaconError.NoAddressError, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, identifier, BeaconError.Aborted, version = version, requestOrigin = requestOrigin),
        errorBeaconResponse(id, identifier, BeaconError.Unknown, version = version, requestOrigin = requestOrigin),
    )

internal fun beaconResponses(version: String = "version", requestOrigin: Origin = Origin.P2P("senderId")): List<BeaconResponse> =
    listOf(
        permissionBeaconResponse(version = version, requestOrigin = requestOrigin),
        blockchainBeaconResponse(version = version, requestOrigin = requestOrigin),
        acknowledgeBeaconResponse(version = version, requestOrigin = requestOrigin),
    ) + errorBeaconResponses(version = version, requestOrigin = requestOrigin)

internal fun beaconVersionedRequests(version: String = "version", senderId: String = "senderId"): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.from(senderId, permissionBeaconRequest(senderId = senderId, version = version)),
        VersionedBeaconMessage.from(senderId, blockchainBeaconRequest(senderId = senderId, version = version)),
    )
