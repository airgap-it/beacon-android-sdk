package it.airgap.beaconsdk.blockchain.substrate.internal.message.v3

import fromValues
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import it.airgap.beaconsdk.blockchain.substrate.data.*
import it.airgap.beaconsdk.blockchain.substrate.internal.creator.*
import it.airgap.beaconsdk.blockchain.substrate.internal.serializer.*
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.SignPayloadSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.request.TransferSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.response.PermissionSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.SignPayloadSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.message.response.TransferSubstrateResponse
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.message.*
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
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        val substrate = Substrate(
            SubstrateCreator(
                DataSubstrateCreator(storageManager, identifierCreator),
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
        val network = SubstrateNetwork(genesisHash = "genesisHash")

        val senderId = "senderId"
        val otherSenderId = "otherSenderId"
        val origin = Origin.P2P(senderId)

        val matchingAppMetadata = SubstrateAppMetadata(senderId, "v3App")
        val otherAppMetadata = SubstrateAppMetadata(otherSenderId, "v3OtherApp")

        val accountId = "accountId"
        val account = SubstrateAccount(accountId, network, "publicKey", "address")

        val otherAccountId = "otherAccountId"
        val otherAccount = SubstrateAccount(otherAccountId, network, "otherPublicKey", "otherAddress")

        val matchingPermission = SubstratePermission(
            accountId,
            senderId,
            0,
            matchingAppMetadata,
            listOf(SubstratePermission.Scope.Transfer, SubstratePermission.Scope.SignPayloadJson, SubstratePermission.Scope.SignPayloadRaw),
            account,
        )

        val otherPermission = SubstratePermission(
            otherAccountId,
            senderId,
            0,
            matchingAppMetadata,
            listOf(SubstratePermission.Scope.Transfer, SubstratePermission.Scope.SignPayloadJson, SubstratePermission.Scope.SignPayloadRaw),
            otherAccount,
        )

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }
        runBlocking { storageManager.setPermissions(listOf(otherPermission, matchingPermission)) }

        runBlocking {
            versionedWithBeacon(
                senderId = senderId,
                appMetadata = matchingAppMetadata,
                origin = origin,
                accountId = accountId,
                account = account,
            )
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
            createSignPayloadRequestJsonPair(),
            createSignPayloadRequestJsonPair(payload = SubstrateSignerPayload.Json(
                blockHash = "blockHash",
                blockNumber = "blockNumber",
                era = "era",
                genesisHash = "genesisHash",
                method = "method",
                nonce = "nonce",
                specVersion = "specVersion",
                tip = "tip",
                transactionVersion = "transactionVersion",
                signedExtensions = emptyList(),
                version = 1,
            )),

            createPermissionResponseJsonPair(),
            createTransferResponseJsonPair(),
            createTransferResponseJsonPair(transactionHash = null),
            createTransferResponseJsonPair(signature = null, payload = null),
            createSignPayloadResponseJsonPair(),
            createSignPayloadResponseJsonPair(transactionHash = null),
            createSignPayloadResponseJsonPair(signature = null, payload = null),
        )

    // -- V2BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "3",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
        appMetadata: SubstrateAppMetadata? = null,
        accountId: String = "accountId",
        account: SubstrateAccount = SubstrateAccount(accountId, SubstrateNetwork("genesisHash"), "publicKey", "address"),
    ): List<Pair<V3BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, senderId = senderId, origin = origin),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, sourceAddress = account.address),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, sourceAddress = account.address, mode = TransferV3SubstrateRequest.Mode.Submit),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, sourceAddress = account.address, mode = TransferV3SubstrateRequest.Mode.SubmitAndReturn),
            createTransferRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, sourceAddress = account.address, mode = TransferV3SubstrateRequest.Mode.Return),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, address = account.address),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, address = account.address, mode = SignPayloadV3SubstrateRequest.Mode.Submit),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, address = account.address, mode = SignPayloadV3SubstrateRequest.Mode.SubmitAndReturn),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, accountId = accountId, address = account.address, mode = SignPayloadV3SubstrateRequest.Mode.Return),

            createPermissionResponsePair(version = version, senderId = senderId, origin = origin),
            createTransferResponsePair(version = version, senderId = senderId, origin = origin),
            createTransferResponsePair(version = version, senderId = senderId, origin = origin, transactionHash = null),
            createTransferResponsePair(version = version, senderId = senderId, origin = origin, signature = null, payload = null),
            createSignPayloadResponsePair(version = version, senderId = senderId, origin = origin),
            createSignPayloadResponsePair(version = version, senderId = senderId, origin = origin, transactionHash = null),
            createSignPayloadResponsePair(version = version, senderId = senderId, origin = origin, signature = null, payload = null),
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
        amount: String = "amount",
        recipient: String = "recipient",
        network: SubstrateNetwork = SubstrateNetwork("genesisHash"),
        mode: TransferV3SubstrateRequest.Mode = TransferV3SubstrateRequest.Mode.SubmitAndReturn,
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                accountId,
                TransferV3SubstrateRequest(
                    SubstratePermission.Scope.Transfer,
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
                        "scope": "transfer",
                        "amount": "$amount",
                        "recipient": "$recipient",
                        "network": ${Json.encodeToString(network)},
                        "mode": ${Json.encodeToString(mode)}
                    }
                }
            }
        """.trimIndent()


    private fun createSignPayloadRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        payload: SubstrateSignerPayload = SubstrateSignerPayload.Raw(
            isMutable = false,
            dataType = SubstrateSignerPayload.Raw.DataType.Bytes,
            data = "data",
        ),
        mode: SignPayloadV3SubstrateRequest.Mode = SignPayloadV3SubstrateRequest.Mode.SubmitAndReturn,
    ): Pair<V3BeaconMessage, String> {
        val scope = when (payload) {
            is SubstrateSignerPayload.Json -> SubstratePermission.Scope.SignPayloadJson
            is SubstrateSignerPayload.Raw -> SubstratePermission.Scope.SignPayloadRaw
        }

        return V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                accountId,
                SignPayloadV3SubstrateRequest(
                    scope,
                    mode,
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
                    "type": "blockchain_request",
                    "accountId": "$accountId",
                    "blockchainData": {
                        "type": "sign_payload_request",
                        "scope": ${Json.encodeToString(scope)},
                        "payload": ${Json.encodeToString(payload)},
                        "mode": ${Json.encodeToString(mode)}
                    }
                }
            }
        """.trimIndent()
    }

    // -- response to JSON --

    private fun createPermissionResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "v3App"),
        scopes: List<SubstratePermission.Scope> = emptyList(),
        accounts: List<SubstrateAccount> = listOf(
            SubstrateAccount("accountId1", SubstrateNetwork("genesisHash"), "publicKey1", "address1"),
            SubstrateAccount("accountId2", SubstrateNetwork("genesisHash"), "publicKey2", "address2"),
        ),
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                Substrate.IDENTIFIER,
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
        signature: String? = "signature",
        payload: String? = "payload",
    ): Pair<V3BeaconMessage, String> {
        val data = mapOf(
            "type" to "transfer_response",
            "transactionHash" to transactionHash,
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
                TransferV3SubstrateResponse(
                    transactionHash,
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

    private fun createSignPayloadResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String? = "transactionHash",
        signature: String? = "signature",
        payload: String? = "payload",
    ): Pair<V3BeaconMessage, String> {
        val data = mapOf(
            "type" to "sign_payload_response",
            "transactionHash" to transactionHash,
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
                SignPayloadV3SubstrateResponse(
                    transactionHash,
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
        sourceAddress: String = "sourceAddress",
        amount: String = "amount",
        recipient: String = "recipient",
        network: SubstrateNetwork = SubstrateNetwork("genesisHash"),
        mode: TransferV3SubstrateRequest.Mode = TransferV3SubstrateRequest.Mode.SubmitAndReturn,
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
                    SubstratePermission.Scope.Transfer,
                    amount,
                    recipient,
                    network,
                    mode,
                ),
            ),
        ) to when (mode) {
            TransferV3SubstrateRequest.Mode.Submit -> TransferSubstrateRequest.Submit(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                sourceAddress,
                amount,
                recipient,
                network,
            )
            TransferV3SubstrateRequest.Mode.SubmitAndReturn -> TransferSubstrateRequest.SubmitAndReturn(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
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
                sourceAddress,
                amount,
                recipient,
                network,
            )
        }

    private fun createSignPayloadRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        address: String = "address",
        payload: SubstrateSignerPayload = SubstrateSignerPayload.Raw(
            isMutable = false,
            dataType = SubstrateSignerPayload.Raw.DataType.Bytes,
            data = "data",
        ),
        mode: SignPayloadV3SubstrateRequest.Mode = SignPayloadV3SubstrateRequest.Mode.SubmitAndReturn,
        appMetadata: SubstrateAppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconRequest> {
        val scope = when (payload) {
            is SubstrateSignerPayload.Json -> SubstratePermission.Scope.SignPayloadJson
            is SubstrateSignerPayload.Raw -> SubstratePermission.Scope.SignPayloadRaw
        }

        return V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Substrate.IDENTIFIER,
                accountId,
                SignPayloadV3SubstrateRequest(
                    scope,
                    mode,
                    payload,
                ),
            ),
        ) to when (mode) {
            SignPayloadV3SubstrateRequest.Mode.Submit -> SignPayloadSubstrateRequest.Submit(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                address,
                payload,
            )
            SignPayloadV3SubstrateRequest.Mode.SubmitAndReturn -> SignPayloadSubstrateRequest.SubmitAndReturn(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                address,
                payload,
            )
            SignPayloadV3SubstrateRequest.Mode.Return -> SignPayloadSubstrateRequest.Return(
                id,
                version,
                Substrate.IDENTIFIER,
                senderId,
                appMetadata,
                origin,
                accountId,
                address,
                payload,
            )
        }
    }

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: SubstrateAppMetadata = SubstrateAppMetadata(senderId, "v3App"),
        scopes: List<SubstratePermission.Scope> = emptyList(),
        accounts: List<SubstrateAccount> = listOf(
            SubstrateAccount("accountId1", SubstrateNetwork("genesisHash"), "publicKey1", "address1"),
            SubstrateAccount("accountId2", SubstrateNetwork("genesisHash"), "publicKey2", "address2"),
        ),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, PermissionBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                Substrate.IDENTIFIER,
                PermissionV3SubstrateResponse(
                    appMetadata,
                    scopes,
                    accounts,
                ),
            ),
        ) to PermissionSubstrateResponse(id, version, origin, Substrate.IDENTIFIER, appMetadata, scopes, accounts)

    private fun createTransferResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String? = "transactionHash",
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
                TransferV3SubstrateResponse(
                    transactionHash,
                    signature,
                    payload,
                ),
            ),
        ) to when {
            transactionHash != null && signature == null && payload == null -> TransferSubstrateResponse.Submit(id, version, origin, Substrate.Companion.IDENTIFIER, transactionHash)
            transactionHash != null && signature != null -> TransferSubstrateResponse.SubmitAndReturn(id, version, origin, Substrate.Companion.IDENTIFIER, transactionHash, signature, payload)
            transactionHash == null && signature != null -> TransferSubstrateResponse.Return(id, version, origin, Substrate.Companion.IDENTIFIER, signature, payload)
            else -> failWithIllegalState("TransferSubstrateResponse cannot be created without `transactionHash` nor `signature`.")
        }

    private fun createSignPayloadResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String? = "transactionHash",
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
                SignPayloadV3SubstrateResponse(
                    transactionHash,
                    signature,
                    payload,
                ),
            ),
        ) to when {
            transactionHash != null && signature == null && payload == null -> SignPayloadSubstrateResponse.Submit(id, version, origin, Substrate.Companion.IDENTIFIER, transactionHash)
            transactionHash != null && signature != null -> SignPayloadSubstrateResponse.SubmitAndReturn(id, version, origin, Substrate.Companion.IDENTIFIER, transactionHash, signature, payload)
            transactionHash == null && signature != null -> SignPayloadSubstrateResponse.Return(id, version, origin, Substrate.Companion.IDENTIFIER, signature, payload)
            else -> failWithIllegalState("SignSubstrateResponse cannot be created without `transactionHash` nor `signature`.")
        }

}