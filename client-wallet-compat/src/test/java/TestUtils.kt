
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.chain.MockChain
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
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    identifier: String = MockChain.IDENTIFIER,
    network: Network = MockNetwork(),
    scopes: List<Permission.Scope> = emptyList(),
    origin: Origin = Origin.P2P(senderId),
    version: String = "version",
): PermissionBeaconRequest = PermissionBeaconRequest(id, senderId, appMetadata, identifier, network, scopes, origin, version)

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
    identifier: String = MockChain.IDENTIFIER,
    network: Network = MockNetwork(),
    scopes: List<Permission.Scope> = emptyList(),
    threshold: Threshold? = null,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): PermissionBeaconResponse = PermissionBeaconResponse(id, publicKey, identifier, network, scopes, threshold, version, requestOrigin)

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
    identifier: String = MockChain.IDENTIFIER,
    errorType: BeaconError = BeaconError.Unknown,
    version: String = "version",
    requestOrigin: Origin = Origin.P2P("senderId"),
): ErrorBeaconResponse = ErrorBeaconResponse(id, identifier, errorType, version, requestOrigin)

internal fun errorBeaconResponses(
    id: String = "id",
    version: String = "version",
    identifier: String = MockChain.IDENTIFIER,
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
        chainBeaconResponse(version = version, requestOrigin = requestOrigin),
        acknowledgeBeaconResponse(version = version, requestOrigin = requestOrigin),
    ) + errorBeaconResponses(version = version, requestOrigin = requestOrigin)

internal fun beaconVersionedRequests(version: String = "version", senderId: String = "senderId"): List<VersionedBeaconMessage> =
    listOf(
        VersionedBeaconMessage.from(senderId, permissionBeaconRequest(senderId = senderId, version = version)),
        VersionedBeaconMessage.from(senderId, chainBeaconRequest(senderId = senderId, version = version)),
    )
