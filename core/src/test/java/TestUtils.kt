
import androidx.annotation.IntRange
import it.airgap.beaconsdk.data.beacon.*
import it.airgap.beaconsdk.data.tezos.TezosActivateAccountOperation
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.message.SerializedBeaconMessage
import it.airgap.beaconsdk.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.internal.utils.Failure
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.message.*
import kotlinx.coroutines.flow.MutableSharedFlow

// -- extensions --

internal fun <T> List<T>.takeHalf(): List<T> = take(size / 2)

internal fun <T> MutableSharedFlow<InternalResult<T>>.tryEmit(messages: List<T>) {
    messages.forEach { tryEmit(Success(it)) }
}

internal fun <T> MutableSharedFlow<InternalResult<T>>.tryEmitFailures(failures: List<Failure<T>>) {
    failures.forEach { tryEmit(it) }
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

internal fun versionedBeaconMessageFlow(
    replay: Int,
): MutableSharedFlow<InternalResult<VersionedBeaconMessage>> = MutableSharedFlow(replay)

internal fun serializedBeaconMessageFlow(
    replay: Int,
): MutableSharedFlow<InternalResult<SerializedBeaconMessage>> = MutableSharedFlow(replay)

// -- factories --

internal fun permissionBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    scopes: List<PermissionScope> = emptyList()
): PermissionBeaconRequest = PermissionBeaconRequest(id, senderId, appMetadata, network, scopes)

internal fun operationBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    operationDetails: TezosOperation = TezosActivateAccountOperation("pkh", "secret"),
    sourceAddress: String = "sourceAddress",
): OperationBeaconRequest = OperationBeaconRequest(id, senderId, appMetadata, network, operationDetails, sourceAddress)

internal fun signPayloadBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    payload: String = "payload",
    sourceAddress: String = "sourceAddress",
): SignPayloadBeaconRequest = SignPayloadBeaconRequest(id, senderId, appMetadata, payload, sourceAddress)

internal fun broadcastBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    signedTransaction: String = "signedTransaction",
): BroadcastBeaconRequest = BroadcastBeaconRequest(id, senderId, appMetadata, network, signedTransaction)

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

internal fun disconnectBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
): DisconnectBeaconMessage = DisconnectBeaconMessage(id, senderId)

internal fun errorBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
    errorType: BeaconException.Type = BeaconException.Type.Unknown,
): ErrorBeaconMessage = ErrorBeaconMessage(id, senderId, errorType)

internal fun beaconErrors(
    id: String = "id",
    senderId: String = "senderId",
): List<ErrorBeaconMessage> =
    listOf(
        errorBeaconMessage(id, senderId, BeaconException.Type.BroadcastError),
        errorBeaconMessage(id, senderId, BeaconException.Type.NetworkNotSupported),
        errorBeaconMessage(id, senderId, BeaconException.Type.NoAddressError),
        errorBeaconMessage(id, senderId, BeaconException.Type.NoPrivateKeyFound),
        errorBeaconMessage(id, senderId, BeaconException.Type.NotGranted),
        errorBeaconMessage(id, senderId, BeaconException.Type.ParametersInvalid),
        errorBeaconMessage(id, senderId, BeaconException.Type.TooManyOperations),
        errorBeaconMessage(id, senderId, BeaconException.Type.TransactionInvalid),
        errorBeaconMessage(id, senderId, BeaconException.Type.Aborted),
        errorBeaconMessage(id, senderId, BeaconException.Type.Unknown),
    )

internal fun beaconResponses(): List<BeaconResponse> =
    listOf(
        permissionBeaconResponse(),
        operationBeaconResponse(),
        signPayloadBeaconResponse(),
        broadcastBeaconResponse(),
    )

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
    if (includeError) add(errorBeaconMessage())
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
    if (includeError) add(VersionedBeaconMessage.fromBeaconMessage(version, senderId, errorBeaconMessage(senderId = senderId)))
}

internal fun p2pPeers(
    @IntRange(from = 1) number: Int = 1,
    version: String? = null,
    paired: Boolean = false,
): List<P2pPeerInfo> =
    (0 until number).map {
        P2pPeerInfo("name#$it", "publicKey#$it", "relayServer#$it", version, paired)
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
    error: Throwable? = null,
): List<Failure<T>> =
    (0 until number).map { Failure(error) }