
import androidx.annotation.IntRange
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchainSerializer
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
): PermissionBeaconRequest = MockBlockchainSerializer.PermissionMockRequest(type, id, version, blockchainIdentifier, senderId, origin, appMetadata)

internal fun blockchainBeaconRequest(
    type: String = "beacon_request",
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    origin: Origin = Origin.P2P(senderId),
    version: String = "version"
): BlockchainBeaconRequest = MockBlockchainSerializer.BlockchainMockRequest(type, id, version, blockchainIdentifier, senderId, appMetadata, origin)

internal fun permissionBeaconResponse(
    type: String = "permission_response",
    id: String = "id",
    publicKey: String = "publicKey",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    threshold: Threshold? = null,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): PermissionBeaconResponse = MockBlockchainSerializer.PermissionMockResponse(type, id, version, requestOrigin, blockchainIdentifier, publicKey, threshold)

internal fun blockchainBeaconResponse(
    type: String = "beacon_response",
    id: String = "id",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): BlockchainBeaconResponse = MockBlockchainSerializer.BlockchainMockResponse(type, id, version, requestOrigin, blockchainIdentifier)

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
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): ErrorBeaconResponse = ErrorBeaconResponse(id, version, requestOrigin, identifier, errorType)

internal fun disconnectBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
    version: String = "version",
    origin: Origin = Origin.P2P("senderId"),
): DisconnectBeaconMessage = DisconnectBeaconMessage(id, senderId, version, origin)

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
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
): List<Permission> =
    appMetadata(number).mapIndexed { index, appMetadata ->
        MockBlockchainSerializer.MockPermission(
            blockchainIdentifier,
            "accountIdentifier#$index",
            "address#$index",
            "sender#$index",
            appMetadata,
            "publicKey#$index",
            index.toLong(),
        )
    }