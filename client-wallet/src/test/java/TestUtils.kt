
import androidx.annotation.IntRange
import it.airgap.beaconsdk.core.data.*
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
    origin: Origin = Origin.P2P(senderId),
    destination: Origin? = Origin.P2P("destination"),
    version: String = "version",
): PermissionBeaconRequest = PermissionMockRequest(type, id, version, blockchainIdentifier, senderId, origin, destination, appMetadata)

internal fun blockchainBeaconRequest(
    type: String = "beacon_request",
    id: String = "id",
    senderId: String = "senderId",
    accountId: String = "accountId",
    appMetadata: AppMetadata = MockAppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Origin = Origin.P2P(senderId),
    destination: Origin? = Origin.P2P("destination"),
    version: String = "version"
): BlockchainBeaconRequest = BlockchainMockRequest(type, id, version, blockchainIdentifier, senderId, appMetadata, origin, destination, accountId)

internal fun permissionBeaconResponse(
    type: String = "permission_response",
    id: String = "id",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    destination: Origin = Origin.P2P("destination"),
): PermissionBeaconResponse = PermissionMockResponse(type, id, version, destination, blockchainIdentifier)

internal fun blockchainBeaconResponse(
    type: String = "beacon_response",
    id: String = "id",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    destination: Origin = Origin.P2P("destination"),
): BlockchainBeaconResponse = BlockchainMockResponse(type, id, version, destination, blockchainIdentifier)

internal fun acknowledgeBeaconResponse(
    id: String = "id",
    senderId: String = "senderId",
    version: String = "version",
    destination: Origin = Origin.P2P("destination"),
): AcknowledgeBeaconResponse =
    AcknowledgeBeaconResponse(id, version, destination, senderId)

internal fun errorBeaconResponse(
    id: String = "id",
    errorType: BeaconError = BeaconError.Unknown,
    description: String? = null,
    version: String = "version",
    destination: Origin = Origin.P2P("destination"),
): ErrorBeaconResponse = ErrorBeaconResponse(id, version, destination, errorType, description)

internal fun disconnectBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
    version: String = "version",
    origin: Origin = Origin.P2P(senderId),
    destination: Origin = Origin.P2P("destination"),
): DisconnectBeaconMessage = DisconnectBeaconMessage(id, senderId, version, origin, destination)

internal fun errorBeaconResponses(
    id: String = "id",
    version: String = "version",
    destination: Origin = Origin.P2P("destination"),
): List<ErrorBeaconResponse> =
    listOf(
        errorBeaconResponse(id, BeaconError.Aborted, version = version, destination = destination),
        errorBeaconResponse(id, BeaconError.Unknown, version = version, destination = destination),
    )

internal fun beaconResponses(version: String = "version", destination: Origin = Origin.P2P("destination")): List<BeaconResponse> =
    listOf(
        permissionBeaconResponse(version = version, destination = destination),
        blockchainBeaconResponse(version = version, destination = destination),
        acknowledgeBeaconResponse(version = version, destination = destination),
    ) + errorBeaconResponses(version = version, destination = destination)

internal fun beaconVersionedRequests(version: String = "version", senderId: String = "senderId", context: VersionedBeaconMessage.Context): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.from(senderId, permissionBeaconRequest(senderId = senderId, version = version), context),
        VersionedBeaconMessage.from(senderId, blockchainBeaconRequest(senderId = senderId, version = version), context),
    )

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