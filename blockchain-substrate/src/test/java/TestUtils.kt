
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAccount
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.response.PermissionSubstrateResponse
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import it.airgap.beaconsdk.core.internal.utils.failWith
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// -- extensions --

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

// -- factories --

internal fun permissionSubstrateRequest(
    id: String = "id",
    version: String = "version",
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    senderId: String = "senderId",
    origin: Connection.Id = Connection.Id.P2P(senderId),
    destination: Connection.Id? = Connection.Id.P2P("receiverId"),
    appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "mockApp"),
    scopes: List<SubstratePermission.Scope> = emptyList(),
    networks: List<SubstrateNetwork> = listOf(SubstrateNetwork("genesisHash")),
): PermissionSubstrateRequest = PermissionSubstrateRequest(id, version, blockchainIdentifier, senderId, origin, destination, appMetadata, scopes, networks)

internal fun permissionSubstrateResponse(
    id: String = "id",
    version: String = "version",
    destination: Connection.Id = Connection.Id.P2P("receiverId"),
    blockchainIdentifier: String = MockBlockchain.IDENTIFIER,
    appMetadata: SubstrateAppMetadata = SubstrateAppMetadata("senderId", "mockApp"),
    scopes: List<SubstratePermission.Scope> = emptyList(),
    accounts: List<SubstrateAccount> = listOf(
      SubstrateAccount("accountId1", SubstrateNetwork("genesisHash"), "publicKey1", "address1"),
      SubstrateAccount("accountId2", SubstrateNetwork("genesisHash"), "publicKey2", "address2"),
    ),
): PermissionSubstrateResponse = PermissionSubstrateResponse(id, version, destination, blockchainIdentifier, appMetadata, scopes, accounts)
