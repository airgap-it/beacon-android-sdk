package it.airgap.beaconsdk.blockchain.substrate.internal.message.v3

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.message.*
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import it.airgap.beaconsdk.blockchain.substrate.data.*
import it.airgap.beaconsdk.blockchain.substrate.internal.creator.*
import it.airgap.beaconsdk.blockchain.substrate.internal.serializer.*
import it.airgap.beaconsdk.blockchain.substrate.internal.wallet.SubstrateWallet
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.SignSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.TransferSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.response.PermissionSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.SignSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.TransferSubstrateResponse
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class V3SubstrateMessageTest {
    @MockK
    private lateinit var wallet: SubstrateWallet

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        val substrate = Substrate(
            wallet,
            SubstrateCreator(
                DataSubstrateCreator(wallet, storageManager, identifierCreator),
                V1BeaconMessageSubstrateCreator(),
                V2BeaconMessageSubstrateCreator(),
                V3BeaconMessageSubstrateCreator(),
            ),
            SubstrateSerializer(
                DataSubstrateSerializer(),
                V1BeaconMessageSubstrateSerializer(),
                V2BeaconMessageSubstrateSerializer(),
                V3BeaconMessageSubstrateSerializer(),
            ),
        )

        val dependencyRegistry = mockDependencyRegistry(substrate)
        every { dependencyRegistry.storageManager } returns storageManager
        every { dependencyRegistry.identifierCreator } returns identifierCreator
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings()
            .map { Json.decodeFromString<V3BeaconMessage>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        messagesWithJsonStrings()
            .map { Json.decodeFromString(JsonObject.serializer(), Json.encodeToString(it.first)) to
                    Json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `is created from Beacon message`() {
        val version = "3"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V3BeaconMessage.from(senderId, it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Origin.P2P(senderId)

        val matchingAppMetadata = SubstrateAppMetadata(senderId, "v3App")
        val otherAppMetadata = SubstrateAppMetadata(otherId, "v3OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(senderId = senderId, appMetadata = matchingAppMetadata, origin = origin)
                .map { it.first.toBeaconMessage(origin) to it.second }
                .forEach {
                    assertEquals(it.second, it.first)
                }
        }
    }

    // -- message to JSON --

    private fun messagesWithJsonStrings() =
        listOf(
            createPermissionRequestJsonPair(),
            createPermissionRequestJsonPair(scopes = listOf(SubstratePermission.Scope.Transfer)),
            createTransferRequestJsonPair(),
            createSignRequestJsonPair(),

            createPermissionResponseJsonPair(),
            createTransferResponseJsonPair(),
            createTransferResponseJsonPair(transactionHash = null),
            createTransferResponseJsonPair(payload = null),
            createSignResponseJsonPair(),
            createSignResponseJsonPair(signature = null),
            createSignResponseJsonPair(payload = null),
        )

    // -- V2BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "3",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
        appMetadata: SubstrateAppMetadata? = null,
    ): List<Pair<V3BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, senderId = senderId, origin = origin),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, mode = TransferV3SubstrateRequest.Mode.Broadcast),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, mode = TransferV3SubstrateRequest.Mode.BroadcastAndReturn),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, mode = TransferV3SubstrateRequest.Mode.Return),
            createSignRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createSignRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, mode = SignV3SubstrateRequest.Mode.Broadcast),
            createSignRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, mode = SignV3SubstrateRequest.Mode.BroadcastAndReturn),
            createSignRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, mode = SignV3SubstrateRequest.Mode.Return),

            createPermissionResponsePair(version = version, senderId = senderId, origin = origin),
            createTransferResponsePair(version = version, senderId = senderId, origin = origin),
            createTransferResponsePair(version = version, senderId = senderId, origin = origin, transactionHash = null),
            createTransferResponsePair(version = version, senderId = senderId, origin = origin, payload = null),
            createSignResponsePair(version = version, senderId = senderId, origin = origin),
            createSignResponsePair(version = version, senderId = senderId, origin = origin, signature = null),
            createSignResponsePair(version = version, senderId = senderId, origin = origin, payload = null),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "v3App"),
        scopes: List<SubstratePermission.Scope> = emptyList(),
        networks: List<SubstrateNetwork> = listOf(SubstrateNetwork("genesisHash")),
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                PermissionV3SubstrateRequest(
                    appMetadata,
                    scopes,
                    networks,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Substrate.IDENTIFIER}",
                    "type": "permission_request",
                    "blockchainData": {
                        "appMetadata": ${Json.encodeToString(appMetadata)},
                        "scopes": ${Json.encodeToString(scopes)},
                        "networks": ${Json.encodeToString(networks)}
                    }
                }
            }
        """.trimIndent()

    private fun createTransferRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        scope: SubstratePermission.Scope = SubstratePermission.Scope.Transfer,
        sourceAddress: String = "sourceAddress",
        amount: String = "amount",
        recipient: String = "recipient",
        network: SubstrateNetwork = SubstrateNetwork("genesisHash"),
        mode: TransferV3SubstrateRequest.Mode = TransferV3SubstrateRequest.Mode.BroadcastAndReturn,
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                accountId,
                TransferV3SubstrateRequest(
                    scope,
                    sourceAddress,
                    amount,
                    recipient,
                    network,
                    mode,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Substrate.IDENTIFIER}",
                    "type": "blockchain_request",
                    "accountId": "$accountId",
                    "blockchainData": {
                        "type": "transfer_request",
                        "scope": ${Json.encodeToString(scope)},
                        "sourceAddress": "$sourceAddress",
                        "amount": "$amount",
                        "recipient": "$recipient",
                        "network": ${Json.encodeToString(network)},
                        "mode": ${Json.encodeToString(mode)}
                    }
                }
            }
        """.trimIndent()


    private fun createSignRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        scope: SubstratePermission.Scope = SubstratePermission.Scope.SignRaw,
        network: SubstrateNetwork = SubstrateNetwork("genesisHash"),
        runtimeSpec: SubstrateRuntimeSpec = SubstrateRuntimeSpec("runtimeVersion", "transactionVersion"),
        payload: String = "payload",
        mode: SignV3SubstrateRequest.Mode = SignV3SubstrateRequest.Mode.BroadcastAndReturn,
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                accountId,
                SignV3SubstrateRequest(
                    scope,
                    network,
                    runtimeSpec,
                    payload,
                    mode,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Substrate.IDENTIFIER}",
                    "type": "blockchain_request",
                    "accountId": "$accountId",
                    "blockchainData": {
                        "type": "sign_request",
                        "scope": ${Json.encodeToString(scope)},
                        "network": ${Json.encodeToString(network)},
                        "runtimeSpec": ${Json.encodeToString(runtimeSpec)},
                        "payload": "$payload",
                        "mode": ${Json.encodeToString(mode)}
                    }
                }
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createPermissionResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountIds: List<String> = listOf("accountId"),
        appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "v3App"),
        scopes: List<SubstratePermission.Scope> = emptyList(),
        accounts: List<SubstrateAccount> = listOf(
            SubstrateAccount(SubstrateNetwork("genesisHash"), 42, "publicKey1"),
            SubstrateAccount(SubstrateNetwork("genesisHash"), 42, "publicKey2"),
        ),
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                Substrate.IDENTIFIER,
                accountIds,
                PermissionV3SubstrateResponse(
                    appMetadata,
                    scopes,
                    accounts,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Substrate.IDENTIFIER}",
                    "type": "permission_response",
                    "accountIds": ${Json.encodeToString(accountIds)},
                    "blockchainData": {
                        "appMetadata": ${Json.encodeToString(appMetadata)},
                        "scopes": ${Json.encodeToString(scopes)},
                        "accounts": ${Json.encodeToString(accounts)}
                    }
                }
            }
        """.trimIndent()

    private fun createTransferResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String? = "transactionHash",
        payload: String? = "payload",
    ): Pair<V3BeaconMessage, String> {
        val data = mapOf(
            "type" to "transfer_response",
            "transactionHash" to transactionHash,
            "payload" to payload,
        )
        
        val dataJson = JsonObject.fromValues(data, includeNulls = false)
        
        return V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Substrate.IDENTIFIER,
                TransferV3SubstrateResponse(
                    transactionHash,
                    payload,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Substrate.IDENTIFIER}",
                    "type": "blockchain_response",
                    "blockchainData": ${Json.encodeToString(dataJson)}
                }
            }
        """.trimIndent()
    }

    private fun createSignResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        signature: String? = "signature",
        payload: String? = "payload",
    ): Pair<V3BeaconMessage, String> {
        val data = mapOf(
            "type" to "sign_response",
            "signature" to signature,
            "payload" to payload,
        )

        val dataJson = JsonObject.fromValues(data, includeNulls = false)

        return V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Substrate.IDENTIFIER,
                SignV3SubstrateResponse(
                    signature,
                    payload,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Substrate.IDENTIFIER}",
                    "type": "blockchain_response",
                    "blockchainData": ${Json.encodeToString(dataJson)}
                }
            }
        """.trimIndent()
    }

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "v3App"),
        scopes: List<SubstratePermission.Scope> = emptyList(),
        networks: List<SubstrateNetwork> = listOf(SubstrateNetwork("genesisHash")),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, PermissionBeaconRequest> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                PermissionV3SubstrateRequest(
                    appMetadata,
                    scopes,
                    networks,
                ),
            ),
        ) to PermissionSubstrateRequest(id, version, Substrate.IDENTIFIER, senderId, origin, appMetadata, scopes, networks)
    
    private fun createTransferRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        scope: SubstratePermission.Scope = SubstratePermission.Scope.Transfer,
        sourceAddress: String = "sourceAddress",
        amount: String = "amount",
        recipient: String = "recipient",
        network: SubstrateNetwork = SubstrateNetwork("genesisHash"),
        mode: TransferV3SubstrateRequest.Mode = TransferV3SubstrateRequest.Mode.BroadcastAndReturn,
        appMetadata: SubstrateAppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconRequest> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                accountId,
                TransferV3SubstrateRequest(
                    scope,
                    sourceAddress,
                    amount,
                    recipient,
                    network,
                    mode,
                ),
            ),
        ) to when (mode) {
            TransferV3SubstrateRequest.Mode.Broadcast -> TransferSubstrateRequest.Broadcast(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                scope,
                sourceAddress,
                amount,
                recipient,
                network,
            )
            TransferV3SubstrateRequest.Mode.BroadcastAndReturn -> TransferSubstrateRequest.BroadcastAndReturn(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                scope,
                sourceAddress,
                amount,
                recipient,
                network,
            )
            TransferV3SubstrateRequest.Mode.Return -> TransferSubstrateRequest.Return(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                scope,
                sourceAddress,
                amount,
                recipient,
                network,
            )
        }

    private fun createSignRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        scope: SubstratePermission.Scope = SubstratePermission.Scope.SignRaw,
        network: SubstrateNetwork = SubstrateNetwork("genesisHash"),
        runtimeSpec: SubstrateRuntimeSpec = SubstrateRuntimeSpec("runtimeVersion", "transactionVersion"),
        payload: String = "payload",
        mode: SignV3SubstrateRequest.Mode = SignV3SubstrateRequest.Mode.BroadcastAndReturn,
        appMetadata: SubstrateAppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconRequest> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                accountId,
                SignV3SubstrateRequest(
                    scope,
                    network,
                    runtimeSpec,
                    payload,
                    mode,
                ),
            ),
        ) to when (mode) {
            SignV3SubstrateRequest.Mode.Broadcast -> SignSubstrateRequest.Broadcast(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                scope,
                network,
                runtimeSpec,
                payload,
            )
            SignV3SubstrateRequest.Mode.BroadcastAndReturn -> SignSubstrateRequest.BroadcastAndReturn(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                scope,
                network,
                runtimeSpec,
                payload,
            )
            SignV3SubstrateRequest.Mode.Return -> SignSubstrateRequest.Return(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                scope,
                network,
                runtimeSpec,
                payload,
            )
        }
    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountIds: List<String> = listOf("accountId"),
        appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "v3App"),
        scopes: List<SubstratePermission.Scope> = emptyList(),
        accounts: List<SubstrateAccount> = listOf(
            SubstrateAccount(SubstrateNetwork("genesisHash"), 42, "publicKey1"),
            SubstrateAccount(SubstrateNetwork("genesisHash"), 42, "publicKey2"),
        ),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, PermissionBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                Substrate.IDENTIFIER,
                accountIds,
                PermissionV3SubstrateResponse(
                    appMetadata,
                    scopes,
                    accounts,
                ),
            ),
        ) to PermissionSubstrateResponse(id, version, origin, Substrate.IDENTIFIER, accountIds, appMetadata, scopes, accounts)

    private fun createTransferResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String? = "transactionHash",
        payload: String? = "payload",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Substrate.IDENTIFIER,
                TransferV3SubstrateResponse(
                    transactionHash,
                    payload,
                ),
            ),
        ) to when {
            transactionHash != null && payload == null -> TransferSubstrateResponse.Broadcast(id, version, origin, Substrate.Companion.IDENTIFIER, transactionHash)
            transactionHash != null && payload != null -> TransferSubstrateResponse.BroadcastAndReturn(id, version, origin, Substrate.Companion.IDENTIFIER, transactionHash, payload)
            transactionHash == null && payload != null -> TransferSubstrateResponse.Return(id, version, origin, Substrate.Companion.IDENTIFIER, payload)
            else -> failWithIllegalState("TransferSubstrateResponse cannot be created without `transactionHash` nor `payload`.")
        }

    private fun createSignResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        signature: String? = "signature",
        payload: String? = "payload",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Substrate.IDENTIFIER,
                SignV3SubstrateResponse(
                    signature,
                    payload,
                ),
            ),
        ) to when {
            signature != null && payload == null -> SignSubstrateResponse.Broadcast(id, version, origin, Substrate.Companion.IDENTIFIER, signature)
            signature != null && payload != null -> SignSubstrateResponse.BroadcastAndReturn(id, version, origin, Substrate.Companion.IDENTIFIER, signature, payload)
            signature == null && payload != null -> SignSubstrateResponse.Return(id, version, origin, Substrate.Companion.IDENTIFIER, payload)
            else -> failWithIllegalState("SignSubstrateResponse cannot be created without `signature` nor `payload`.")
        }

}