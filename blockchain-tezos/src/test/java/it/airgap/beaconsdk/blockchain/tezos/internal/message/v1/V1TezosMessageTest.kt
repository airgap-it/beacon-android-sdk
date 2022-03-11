package it.airgap.beaconsdk.blockchain.tezos.internal.message.v1

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
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.SigningType
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
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

internal class V1TezosMessageTest {
    @MockK
    private lateinit var tezosWallet: TezosWallet

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { identifierCreator.accountId(any(), any()) } answers { Result.success(firstArg()) }

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
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
        every { tezosWallet.address(any()) } answers { Result.success(firstArg()) }
    }

    @Test
    fun `is deserialized from JSON`() {
        messagesWithJsonStrings()
            .map { Json.decodeFromString<V1TezosMessage>(it.second) to it.first }
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
        val version = "1"
        val senderId = "senderId"

        versionedWithBeacon(version, senderId)
            .map { V1TezosMessage.from(senderId, it.second) to it.first }
            .forEach {
                assertEquals(it.second, it.first)
            }
    }

    @Test
    fun `converts to Beacon message`() {
        val senderId = "senderId"
        val otherId = "otherId"
        val origin = Origin.P2P("v1App")

        val matchingAppMetadata = TezosAppMetadata(senderId, "v1App")
        val otherAppMetadata = TezosAppMetadata(otherId, "v1OtherApp")

        runBlocking { storageManager.setAppMetadata(listOf(otherAppMetadata, matchingAppMetadata)) }

        runBlocking {
            versionedWithBeacon(beaconId = senderId, appMetadata = matchingAppMetadata, origin = origin)
                .map { it.first.toBeaconMessage(origin) to it.second }
                .forEach {
                    assertEquals(it.second, it.first)
                }
        }
    }

    // -- message to JSON --

    private fun messagesWithJsonStrings() = listOf(
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

    // -- V1TezosMessage to BeaconMessage --

    private fun versionedWithBeacon(
        version: String = "1",
        beaconId: String = "beaconId",
        origin: Origin = Origin.P2P(beaconId),
        appMetadata: TezosAppMetadata? = null,
    ): List<Pair<V1BeaconMessage, BeaconMessage>> =
        listOf(
            createPermissionRequestPair(version = version, beaconId = beaconId, origin = origin),
            createOperationRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),
            createOperationRequestPair(
                version = version,
                beaconId = beaconId,
                tezosOperations = listOf(TezosEndorsementOperation("level")),
                appMetadata = appMetadata,
                origin = origin,
            ),
            createSignPayloadRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),
            createBroadcastRequestPair(version = version, beaconId = beaconId, appMetadata = appMetadata, origin = origin),

            createPermissionResponsePair(version = version, beaconId = beaconId, origin = origin),
            createOperationResponsePair(version = version, beaconId = beaconId, origin = origin),
            createSignPayloadResponsePair(version = version, beaconId = beaconId, origin = origin),
            createBroadcastResponsePair(version = version, beaconId = beaconId, origin = origin),
        )

    // -- request to JSON --

    private fun createPermissionRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        appMetadata: V1TezosAppMetadata = V1TezosAppMetadata(beaconId, "v1App"),
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList()
    ): Pair<PermissionV1TezosRequest, String> =
        PermissionV1TezosRequest(version, id, beaconId, appMetadata, network, scopes) to """
            {
                "type": "permission_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "appMetadata": ${Json.encodeToString(appMetadata)},
                "network": ${Json.encodeToString(network)},
                "scopes": ${Json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: TezosNetwork = TezosNetwork.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress"
    ): Pair<OperationV1TezosRequest, String> =
        OperationV1TezosRequest(version, id, beaconId, network, tezosOperations, sourceAddress) to """
            {
                "type": "operation_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "network": ${Json.encodeToString(network)},
                "operationDetails": ${Json.encodeToString(tezosOperations)},
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createSignPayloadRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
    ): Pair<SignPayloadV1TezosRequest, String> =
        SignPayloadV1TezosRequest(version, id, beaconId, payload, sourceAddress) to """
            {
                "type": "sign_payload_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "payload": "$payload",
                "sourceAddress": "$sourceAddress"
            }
        """.trimIndent()

    private fun createBroadcastRequestJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: TezosNetwork = TezosNetwork.Custom(),
        signedTransaction: String = "signedTransaction"
    ): Pair<BroadcastV1TezosRequest, String> =
        BroadcastV1TezosRequest(version, id, beaconId, network, signedTransaction) to """
            {
                "type": "broadcast_request",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "network": ${Json.encodeToString(network)},
                "signedTransaction": "$signedTransaction"
            }
        """.trimIndent()

    // -- response to JSON --

    private fun createPermissionResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        publicKey: String = "publicKey",
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
    ): Pair<PermissionV1TezosResponse, String> =
        PermissionV1TezosResponse(version, id, beaconId, publicKey, network, scopes) to """
            {
                "type": "permission_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "publicKey": "$publicKey",
                "network": ${Json.encodeToString(network)},
                "scopes": ${Json.encodeToString(scopes)}
            }
        """.trimIndent()

    private fun createOperationResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<OperationV1TezosResponse, String> =
        OperationV1TezosResponse(version, id, beaconId, transactionHash) to """
            {
                "type": "operation_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    private fun createSignPayloadResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        signature: String = "signature",
    ): Pair<SignPayloadV1TezosResponse, String> =
        SignPayloadV1TezosResponse(version, id, beaconId, signature) to """
            {
                "type": "sign_payload_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "signature": "$signature"
            }
        """.trimIndent()

    private fun createBroadcastResponseJsonPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash"
    ): Pair<BroadcastV1TezosResponse, String> =
        BroadcastV1TezosResponse(version, id, beaconId, transactionHash) to """
            {
                "type": "broadcast_response",
                "version": "$version",
                "id": "$id",
                "beaconId": "$beaconId",
                "transactionHash": "$transactionHash"
            }
        """.trimIndent()

    // -- request to BeaconMessage --

    private fun createPermissionRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        appMetadata: V1TezosAppMetadata = V1TezosAppMetadata("beaconId", "v1App"),
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<PermissionV1TezosRequest, PermissionBeaconRequest> =
        PermissionV1TezosRequest(version, id, beaconId, appMetadata, network, scopes) to
            PermissionTezosRequest(id, version, Tezos.IDENTIFIER, beaconId, appMetadata.toAppMetadata(), origin, network, scopes)

    private fun createOperationRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: TezosNetwork = TezosNetwork.Custom(),
        tezosOperations: List<TezosOperation> = emptyList(),
        sourceAddress: String = "sourceAddress",
        appMetadata: TezosAppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<OperationV1TezosRequest, BlockchainBeaconRequest> =
        OperationV1TezosRequest(version, id, beaconId, network, tezosOperations, sourceAddress) to
                OperationTezosRequest(id, version, Tezos.IDENTIFIER, beaconId, appMetadata, origin, null, network, tezosOperations, sourceAddress)

    private fun createSignPayloadRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        payload: String = "payload",
        sourceAddress: String = "sourceAddress",
        appMetadata: TezosAppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<SignPayloadV1TezosRequest, BlockchainBeaconRequest> =
        SignPayloadV1TezosRequest(version, id, beaconId, payload, sourceAddress) to
                SignPayloadTezosRequest(id, version, Tezos.IDENTIFIER, beaconId, appMetadata, origin, null, SigningType.Raw, payload, sourceAddress)

    private fun createBroadcastRequestPair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        network: TezosNetwork = TezosNetwork.Custom(),
        signedTransaction: String = "signedTransaction",
        appMetadata: TezosAppMetadata? = null,
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<BroadcastV1TezosRequest, BlockchainBeaconRequest> =
        BroadcastV1TezosRequest(version, id, beaconId, network, signedTransaction) to
                BroadcastTezosRequest(id, version, Tezos.IDENTIFIER, beaconId, appMetadata, origin, null, network, signedTransaction)

    // -- response to BeaconMessage --

    private fun createPermissionResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        publicKey: String = "publicKey",
        network: TezosNetwork = TezosNetwork.Custom(),
        scopes: List<TezosPermission.Scope> = emptyList(),
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<PermissionV1TezosResponse, PermissionBeaconResponse> =
        PermissionV1TezosResponse(version, id, beaconId, publicKey, network, scopes) to
            PermissionTezosResponse(id, version, origin, Tezos.IDENTIFIER, TezosAccount(publicKey, network, publicKey, publicKey), scopes)

    private fun createOperationResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<OperationV1TezosResponse, BlockchainBeaconResponse> =
        OperationV1TezosResponse(version, id, beaconId, transactionHash) to
                OperationTezosResponse(id, version, origin, Tezos.IDENTIFIER, transactionHash)

    private fun createSignPayloadResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        signature: String = "signature",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<SignPayloadV1TezosResponse, BlockchainBeaconResponse> =
        SignPayloadV1TezosResponse(version, id, beaconId, signature) to
                SignPayloadTezosResponse(id, version, origin, Tezos.IDENTIFIER, SigningType.Raw, signature)

    private fun createBroadcastResponsePair(
        version: String = "1",
        id: String = "id",
        beaconId: String = "beaconId",
        transactionHash: String = "transactionHash",
        origin: Origin = Origin.P2P(beaconId),
    ): Pair<BroadcastV1TezosResponse, BlockchainBeaconResponse> =
        BroadcastV1TezosResponse(version, id, beaconId, transactionHash) to
            BroadcastTezosResponse(id, version, origin, Tezos.IDENTIFIER, transactionHash)
}