package it.airgap.beaconsdk.blockchain.tezos.internal.message.v2

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
import it.airgap.beaconsdk.blockchain.tezos.internal.di.extend
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
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.compat.CoreCompat
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.internal.serializer.contextualJson
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.message.*
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mockDependencyRegistry
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class V2TezosMessageTest {
    @MockK
    private lateinit var tezosWallet: TezosWallet

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager
    private lateinit var json: Json

    private val beaconScope: BeaconScope = BeaconScope.Global

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { identifierCreator.accountId(any(), any()) } answers { Result.success(firstArg()) }

        storageManager = StorageManager(beaconScope, MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))

        val tezos = Tezos(
            tezosWallet,
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
        every { dependencyRegistry.extend().tezosWallet.address(any()) } answers { Result.success(firstArg()) }

        json = contextualJson(dependencyRegistry.blockchainRegistry, CoreCompat(beaconScope))
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings()
            .map { json.decodeFromString<V2TezosMessage>(it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `serializes to JSON`() {
        messagesWithJsonStrings()
            .map { json.decodeFromString(JsonObject.serializer(), json.encodeToString(it.first)) to
                    json.decodeFromString(JsonObject.serializer(), it.second) }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `is created from Beacon message`() {
        val version = "2"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V2TezosMessage.from(senderId, it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Connection.Id.P2P(senderId)
        val destination = Connection.Id.P2P("receiverId")

        val matchingAppMetadata = TezosAppMetadata(senderId, "v2App")
        val otherAppMetadata = TezosAppMetadata(otherId, "v2OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(senderId = senderId, appMetadata = matchingAppMetadata, origin = origin, destination = destination)
                .map { it.first.toBeaconMessage(origin, destination, beaconScope) to it.second }
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
        version: String = "2",
        senderId: String = "senderId",
        origin: Connection.Id = Connection.Id.P2P(senderId),
        destination: Connection.Id = Connection.Id.P2P("receiverId"),
        appMetadata: TezosAppMetadata? = null,
    ): List<Pair<V2BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, senderId = senderId, origin = origin, destination = destination),
            createOperationRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, destination = destination),
            createOperationRequestPair(
                version = version,
                senderId = senderId,
                tezosOperations = listOf(TezosEndorsementOperation("level")),
                appMetadata = appMetadata,
                origin = origin,
                destination = destination,
            ),
            createSignPayloadRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, destination = destination),
            createBroadcastRequestPair(version = version, senderId = senderId, appMetadata = appMetadata, origin = origin, destination = destination),

            createPermissionResponsePair(version = version, senderId = senderId, destination = destination),
            createOperationResponsePair(version = version, senderId = senderId, destination = destination),
            createSignPayloadResponsePair(version = version, senderId = senderId, destination = destination),
            createBroadcastResponsePair(version = version, senderId = senderId, destination = destination),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: V2TezosAppMetadata = V2TezosAppMetadata(senderId, "v2App"),
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList()
    ): Pair<PermissionV2TezosRequest, String> =
        PermissionV2TezosRequest(version, id, senderId, appMetadata, network, scopes) to """
            {
                "type": "permission_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "appMetadata": ${json.encodeToString(appMetadata)},
                "network": ${json.encodeToString(network)},
                "scopes": ${json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: TezosNetwork = TezosNetwork.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<OperationV2TezosRequest, String> =
        OperationV2TezosRequest(version, id, senderId, network, tezosOperations, sourceAddress) to """
            {
                "type": "operation_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "network": ${json.encodeToString(network)},
                "operationDetails": ${json.encodeToString(tezosOperations)},
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createSignPayloadRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
    ): Pair<SignPayloadV2TezosRequest, String> =
        SignPayloadV2TezosRequest(version, id, senderId, signingType, payload, sourceAddress) to """
            {
                "type": "sign_payload_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "signingType": ${json.encodeToString(signingType)},
                "payload": "$payload",
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createBroadcastRequestJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: TezosNetwork = TezosNetwork.Custom(),
        signedTransaction: String = "signedTransaction"
    ): Pair<BroadcastV2TezosRequest, String> =
        BroadcastV2TezosRequest(version, id, senderId, network, signedTransaction) to """
            {
                "type": "broadcast_request",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "network": ${json.encodeToString(network)},
                "signedTransaction": "$signedTransaction"
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createPermissionResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        publicKey: String = "publicKey",
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
    ): Pair<PermissionV2TezosResponse, String> =
        PermissionV2TezosResponse(version, id, senderId, publicKey, network, scopes) to """
            {
                "type": "permission_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "publicKey": "$publicKey",
                "network": ${json.encodeToString(network)},
                "scopes": ${json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<OperationV2TezosResponse, String> =
        OperationV2TezosResponse(version, id, senderId, transactionHash) to """
            {
                "type": "operation_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    private fun createSignPayloadResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
    ): Pair<SignPayloadV2TezosResponse, String> =
        SignPayloadV2TezosResponse(version, id, senderId, signingType, signature) to """
            {
                "type": "sign_payload_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "signingType": ${json.encodeToString(signingType)},
                "signature": "$signature"
            }
        """.trimIndent()

    private fun createBroadcastResponseJsonPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash"
    ): Pair<BroadcastV2TezosResponse, String> =
        BroadcastV2TezosResponse(version, id, senderId, transactionHash) to """
            {
                "type": "broadcast_response",
                "version": "$version",
                "id": "$id",
                "senderId": "$senderId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        appMetadata: V2TezosAppMetadata = V2TezosAppMetadata("senderId", "v2App"),
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
        origin: Connection.Id = Connection.Id.P2P(senderId),
        destination: Connection.Id? = Connection.Id.P2P("receiverId"),
    ): Pair<PermissionV2TezosRequest, PermissionBeaconRequest> =
        PermissionV2TezosRequest(version, id, senderId, appMetadata, network, scopes) to
            PermissionTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata.toAppMetadata(), origin, destination, network, scopes)

    private fun createOperationRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: TezosNetwork = TezosNetwork.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: TezosAppMetadata? = null,
        origin: Connection.Id = Connection.Id.P2P(senderId),
        destination: Connection.Id? = Connection.Id.P2P("receiverId"),
    ): Pair<OperationV2TezosRequest, BlockchainBeaconRequest> =
        OperationV2TezosRequest(version, id, senderId, network, tezosOperations, sourceAddress) to
                OperationTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata, origin, destination, null, network, tezosOperations, sourceAddress)

    private fun createSignPayloadRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: TezosAppMetadata? = null,
        origin: Connection.Id = Connection.Id.P2P(senderId),
        destination: Connection.Id? = Connection.Id.P2P("receiverId"),
    ): Pair<SignPayloadV2TezosRequest, BlockchainBeaconRequest> =
        SignPayloadV2TezosRequest(version, id, senderId, signingType, payload, sourceAddress) to
                SignPayloadTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata, origin, destination, null, signingType, payload, sourceAddress)

    private fun createBroadcastRequestPair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        network: TezosNetwork = TezosNetwork.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: TezosAppMetadata? = null,
        origin: Connection.Id = Connection.Id.P2P(senderId),
        destination: Connection.Id? = Connection.Id.P2P("receiverId"),
    ): Pair<BroadcastV2TezosRequest, BlockchainBeaconRequest> =
        BroadcastV2TezosRequest(version, id, senderId, network, signedTransaction) to
                BroadcastTezosRequest(id, version, Tezos.IDENTIFIER, senderId, appMetadata, origin, destination, null, network, signedTransaction)

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        publicKey: String = "publicKey",
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
        destination: Connection.Id = Connection.Id.P2P("receiverId"),
    ): Pair<PermissionV2TezosResponse, PermissionBeaconResponse> =
        PermissionV2TezosResponse(version, id, senderId, publicKey, network, scopes) to
            PermissionTezosResponse(id, version, destination, Tezos.IDENTIFIER, TezosAccount(publicKey, network, publicKey, publicKey), scopes)

    private fun createOperationResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        destination: Connection.Id = Connection.Id.P2P("receiverId"),
    ): Pair<OperationV2TezosResponse, BlockchainBeaconResponse> =
        OperationV2TezosResponse(version, id, senderId, transactionHash) to
                OperationTezosResponse(id, version, destination, Tezos.Companion.IDENTIFIER, transactionHash)

    private fun createSignPayloadResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        signingType: SigningType = SigningType.Raw,
        signature: String = "signature",
        destination: Connection.Id = Connection.Id.P2P("receiverId"),
    ): Pair<SignPayloadV2TezosResponse, BlockchainBeaconResponse> =
        SignPayloadV2TezosResponse(version, id, senderId, signingType, signature) to
                SignPayloadTezosResponse(id, version, destination, Tezos.Companion.IDENTIFIER, signingType, signature)

    private fun createBroadcastResponsePair(
        version: String = "2",
        id: String = "id",
        senderId: String = "senderId",
        transactionHash: String = "transactionHash",
        destination: Connection.Id = Connection.Id.P2P("receiverId"),
    ): Pair<BroadcastV2TezosResponse, BlockchainBeaconResponse> =
        BroadcastV2TezosResponse(version, id, senderId, transactionHash) to
                BroadcastTezosResponse(id, version, destination, Tezos.IDENTIFIER, transactionHash)

}