package it.airgap.beaconsdk.blockchain.tezos.internal.message.v3

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAccount
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosEndorsementOperation
import it.airgap.beaconsdk.blockchain.tezos.data.operation.TezosOperation
import it.airgap.beaconsdk.blockchain.tezos.internal.creator.*
import it.airgap.beaconsdk.blockchain.tezos.internal.serializer.*
import it.airgap.beaconsdk.blockchain.tezos.internal.wallet.TezosWallet
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.BroadcastTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.OperationTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.message.response.SignPayloadTezosResponse
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.message.v3.*
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
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

internal class V3TezosMessageTest {
    @MockK
    private lateinit var wallet: TezosWallet

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
        val tezos = Tezos(
            wallet,
            TezosCreator(
                DataTezosCreator(storageManager, identifierCreator),
                V1BeaconMessageTezosCreator(),
                V2BeaconMessageTezosCreator(),
                V3BeaconMessageTezosCreator(),
            ),
            TezosSerializer(
                DataTezosSerializer(),
                V1BeaconMessageTezosSerializer(),
                V2BeaconMessageTezosSerializer(),
                V3BeaconMessageTezosSerializer(),
            ),
        )

        val dependencyRegistry = mockDependencyRegistry(tezos)
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

        val matchingAppMetadata = TezosAppMetadata(senderId, "v3App")
        val otherAppMetadata = TezosAppMetadata(otherId, "v3OtherApp")

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
            createPermissionRequestJsonPair(scopes = listOf(TezosPermission.Scope.Sign)),
            createOperationRequestJsonPair(),
            createOperationRequestJsonPair(tezosOperations = listOf(TezosEndorsementOperation("level"))),
            createSignPayloadRequestJsonPair(),
            createBroadcastRequestJsonPair(),

            createPermissionResponseJsonPair(),
            createOperationResponseJsonPair(),
            createSignPayloadResponseJsonPair(),
            createBroadcastResponseJsonPair(),
        )

    // -- V2BeaconMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "3",
        senderId: String = "senderId",
        origin: Origin = Origin.P2P(senderId),
        appMetadata: TezosAppMetadata? = null,
    ): List<Pair<V3BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, senderId = senderId, origin = origin),
            createOperationRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createOperationRequestPair(
                version = version,
                senderId = senderId,
                tezosOperations = listOf(TezosEndorsementOperation("level")),
                appMetadata = appMetadata,
                origin = origin,
            ),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),
            createBroadcastRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin),

            createPermissionResponsePair(version = version, senderId = senderId, origin = origin),
            createOperationResponsePair(version = version, senderId = senderId, origin = origin),
            createSignPayloadResponsePair(version = version, senderId = senderId, origin = origin),
            createBroadcastResponsePair(version = version, senderId = senderId, origin = origin),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: V3TezosAppMetadata = V3TezosAppMetadata(senderId, "v3App"),
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList()
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                PermissionV3TezosRequest(
                    network,
                    appMetadata,
                    scopes,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "permission_request",
                    "blockchainData": {
                        "appMetadata": ${Json.encodeToString(appMetadata)},
                        "network": ${Json.encodeToString(network)},
                        "scopes": ${Json.encodeToString(scopes)}
                    }
                }
            }
        """.trimIndent()

    private fun createOperationRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        network: TezosNetwork = TezosNetwork.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                accountId,
                OperationV3TezosRequest(
                    network,
                    tezosOperations,
                    sourceAddress,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "blockchain_request",
                    "accountId": "$accountId",
                    "blockchainData": {
                        "type": "operation_request",
                        "network": ${Json.encodeToString(network)},
                        "operationDetails": ${Json.encodeToString(tezosOperations)},
                        "sourceAddress": "$sourceAddress"
                    }
                }
            }
        """.trimIndent()

    private fun createSignPayloadRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                accountId,
                SignPayloadV3TezosRequest(
                    signingType,
                    payload,
                    sourceAddress,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "blockchain_request",
                    "accountId": "$accountId",
                    "blockchainData": {
                        "type": "sign_payload_request",
                        "signingType": ${Json.encodeToString(signingType)},
                        "payload": "$payload",
                        "sourceAddress": "$sourceAddress"
                    }
                }
            }
        """.trimIndent()


    private fun createBroadcastRequestJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        network: TezosNetwork = TezosNetwork.Custom(),
        signedTransaction: String = "signedTransaction"
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                accountId,
                BroadcastV3TezosRequest(
                    network,
                    signedTransaction,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "blockchain_request",
                    "accountId": "$accountId",
                    "blockchainData": {
                        "type": "broadcast_request",
                        "network": ${Json.encodeToString(network)},
                        "signedTransaction": "$signedTransaction"
                    }
                }
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createPermissionResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        publicKey: String = "publicKey",
        address: String = "address",
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                PermissionV3TezosResponse(
                    accountId,
                    publicKey,
                    address,
                    network,
                    scopes,
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "permission_response",
                    "blockchainData": {
                        "accountId": "$accountId",
                        "publicKey": "$publicKey",
                        "address": "$address",
                        "network": ${Json.encodeToString(network)},
                        "scopes": ${Json.encodeToString(scopes)}
                    }
                }
            }
        """.trimIndent()

    private fun createOperationResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                OperationV3TezosResponse(transactionHash),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "blockchain_response",
                    "blockchainData": {
                        "type": "operation_response",
                        "transactionHash": "$transactionHash"
                    }
                }
            }
        """.trimIndent()

    private fun createSignPayloadResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                SignPayloadV3TezosResponse(
                    signingType,
                    signature
                ),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "blockchain_response",
                    "blockchainData": {
                        "type": "sign_payload_response",
                        "signingType": ${Json.encodeToString(signingType)},
                        "signature": "$signature"
                    }
                }
            }
        """.trimIndent()

    private fun createBroadcastResponseJsonPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<V3BeaconMessage, String> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                BroadcastV3TezosResponse(transactionHash),
            ),
        ) to """
            {
                "id": "$id",
                "version": "$version",
                "senderId": "$senderId",
                "message": {
                    "blockchainIdentifier": "${Tezos.IDENTIFIER}",
                    "type": "blockchain_response",
                    "blockchainData": {
                        "type": "broadcast_response",
                        "transactionHash": "$transactionHash"   
                    }
                }
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: V3TezosAppMetadata = V3TezosAppMetadata("senderId", "v2App"),
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, PermissionBeaconRequest> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                PermissionV3TezosRequest(
                    network,
                    appMetadata,
                    scopes,
                ),
            ),
        ) to PermissionTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata.toAppMetadata(), origin, network, scopes)

    private fun createOperationRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        network: TezosNetwork = TezosNetwork.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: TezosAppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconRequest> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                accountId,
                OperationV3TezosRequest(
                    network,
                    tezosOperations,
                    sourceAddress,
                ),
            ),
        ) to OperationTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata, origin, accountId, network, tezosOperations, sourceAddress)

    private fun createSignPayloadRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: TezosAppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconRequest> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                accountId,
                SignPayloadV3TezosRequest(
                    signingType,
                    payload,
                    sourceAddress,
                ),
            ),
        ) to SignPayloadTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata, origin, accountId, signingType, payload, sourceAddress)

    private fun createBroadcastRequestPair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        accountId: String = "accountId",
        network: TezosNetwork = TezosNetwork.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: TezosAppMetadata? = null,
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconRequest> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconRequestContent(
                Tezos.IDENTIFIER,
                accountId,
                BroadcastV3TezosRequest(
                    network,
                    signedTransaction,
                ),
            ),
        ) to BroadcastTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata, origin, accountId, network, signedTransaction)

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        account: TezosAccount = TezosAccount("accountId", TezosNetwork.Custom(), "publicKey", "address"),
        scopes: List<TezosPermission.Scope> = emptyList(),
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, PermissionBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            PermissionV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                PermissionV3TezosResponse(
                    account.accountId,
                    account.publicKey,
                    account.address,
                    account.network,
                    scopes,
                ),
            ),
        ) to PermissionTezosResponse(id, version, origin, Tezos.IDENTIFIER, account, scopes)

    private fun createOperationResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                OperationV3TezosResponse(transactionHash),
            ),
        ) to OperationTezosResponse(id, version, origin, Tezos.Companion.IDENTIFIER, transactionHash)

    private fun createSignPayloadResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                SignPayloadV3TezosResponse(
                    signingType,
                    signature
                ),
            ),
        ) to SignPayloadTezosResponse(id, version, origin, Tezos.Companion.IDENTIFIER, signingType, signature)

    private fun createBroadcastResponsePair(
        version: String = "3",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(senderId),
    ): Pair<V3BeaconMessage, BlockchainBeaconResponse> =
        V3BeaconMessage(
            id,
            version,
            senderId,
            BlockchainV3BeaconResponseContent(
                Tezos.IDENTIFIER,
                BroadcastV3TezosResponse(transactionHash),
            ),
        ) to BroadcastTezosResponse(id, version, origin, Tezos.IDENTIFIER, transactionHash)

}