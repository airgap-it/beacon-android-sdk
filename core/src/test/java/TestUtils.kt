
import it.airgap.beaconsdk.data.beacon.AppMetadata
import it.airgap.beaconsdk.data.beacon.Network
import it.airgap.beaconsdk.data.beacon.PermissionScope
import it.airgap.beaconsdk.data.tezos.TezosOperation
import it.airgap.beaconsdk.exception.BeaconException
import it.airgap.beaconsdk.internal.utils.InternalResult
import it.airgap.beaconsdk.internal.utils.Success
import it.airgap.beaconsdk.message.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.lang.Integer.max

// -- extensions --

internal fun <T> List<T>.takeHalf(): List<T> = take(max(size / 2, 1))

fun String.removeWhitespace(): String = replace(Regex("[\\s+|\n]"), "")

internal fun <T> MutableSharedFlow<InternalResult<T>>.tryEmit(messages: List<T>) {
    messages.forEach { tryEmit(Success(it)) }
}

// -- factories --

internal fun createPermissionBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    scopes: List<PermissionScope> = emptyList()
): PermissionBeaconRequest = PermissionBeaconRequest(id, senderId, appMetadata, network, scopes)

internal fun createOperationBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    operationDetails: TezosOperation = TezosOperation.ActivateAccount("pkh", "secret"),
    sourceAddress: String = "sourceAddress",
): OperationBeaconRequest = OperationBeaconRequest(id, senderId, appMetadata, network, operationDetails, sourceAddress)

internal fun createSignPayloadBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    payload: String = "payload",
    sourceAddress: String = "sourceAddress",
): SignPayloadBeaconRequest = SignPayloadBeaconRequest(id, senderId, appMetadata, payload, sourceAddress)

internal fun createBroadcastBeaconRequest(
    id: String = "id",
    senderId: String = "senderId",
    appMetadata: AppMetadata? = AppMetadata(senderId, "mockApp"),
    network: Network = Network.Custom(),
    signedTransaction: String = "signedTransaction",
): BroadcastBeaconRequest = BroadcastBeaconRequest(id, senderId, appMetadata, network, signedTransaction)

internal fun createPermissionBeaconResponse(
    id: String = "id",
    publicKey: String = "publicKey",
    network: Network = Network.Custom(),
    scopes: List<PermissionScope> = emptyList(),
): PermissionBeaconResponse = PermissionBeaconResponse(id, publicKey, network, scopes)

internal fun createOperationBeaconResponse(
    id: String = "id",
    transactionHash: String = "transactionHash",
): OperationBeaconResponse = OperationBeaconResponse(id, transactionHash)

internal fun createSignPayloadBeaconResponse(
    id: String = "id",
    signature: String = "signature",
): SignPayloadBeaconResponse = SignPayloadBeaconResponse(id, signature)

internal fun createBroadcastBeaconResponse(
    id: String = "id",
    transactionHash: String = "transactionHash",
): BroadcastBeaconResponse = BroadcastBeaconResponse(id, transactionHash)

internal fun createDisconnectBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
): DisconnectBeaconMessage = DisconnectBeaconMessage(id, senderId)

internal fun createErrorBeaconMessage(
    id: String = "id",
    senderId: String = "senderId",
    errorType: BeaconException.Type = BeaconException.Type.Unknown,
): ErrorBeaconMessage = ErrorBeaconMessage(id, senderId, errorType)