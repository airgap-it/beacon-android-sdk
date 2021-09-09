package it.airgap.beaconsdk.internal.di

import it.airgap.beaconsdk.data.beacon.Connection
import it.airgap.beaconsdk.data.beacon.P2P
import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.BeaconSdk
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.internal.crypto.provider.LazySodiumCryptoProvider
import it.airgap.beaconsdk.internal.migration.Migration
import it.airgap.beaconsdk.internal.migration.v1_0_4.MigrationFromV1_0_4
import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.provider.KtorHttpProvider
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.serializer.provider.Base58CheckSerializerProvider
import it.airgap.beaconsdk.internal.serializer.provider.SerializerProvider
import it.airgap.beaconsdk.internal.storage.SecureStorage
import it.airgap.beaconsdk.internal.storage.Storage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.transport.p2p.P2pClient
import it.airgap.beaconsdk.internal.transport.p2p.P2pTransport
import it.airgap.beaconsdk.internal.transport.p2p.matrix.MatrixClient
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.event.MatrixEventService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.node.MatrixNodeService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.room.MatrixRoomService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.user.MatrixUserService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.MatrixStore
import it.airgap.beaconsdk.internal.transport.p2p.store.P2pStore
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCommunicator
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCrypto
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Base58Check
import it.airgap.beaconsdk.internal.utils.Poller
import it.airgap.beaconsdk.network.provider.HttpProvider

internal class DependencyRegistry(storage: Storage, secureStorage: SecureStorage) {

    // -- storage --
    val storageManager: StorageManager by lazy { StorageManager(storage, secureStorage, accountUtils) }

    // -- protocol --

    val protocolRegistry: ProtocolRegistry by lazy { ProtocolRegistry(crypto, base58Check) }

    // -- controller --

    val messageController: MessageController by lazy { MessageController(protocolRegistry, storageManager, accountUtils) }

    fun connectionController(connections: List<Connection>): ConnectionController {
            val transports = connections.map { transport(it) }

            return ConnectionController(transports, serializer)
        }

    // -- transport --

    fun transport(connection: Connection): Transport =
        when (connection) {
            is P2P -> {
                val app = BeaconSdk.instance.app

                val matrixClient = matrixClient(connection.httpProvider)
                val matrixNodes = connection.nodes

                val p2pCommunicator = P2pCommunicator(app, crypto)
                val p2pStore = P2pStore(app, p2pCommunicator, matrixClient, matrixNodes, storageManager, migration)
                val p2pCrypto = P2pCrypto(app, crypto)

                val client = P2pClient(
                    matrixClient,
                    p2pStore,
                    p2pCrypto,
                    p2pCommunicator,
                )

                P2pTransport(storageManager, client)
            }
        }

    // -- utils --

    val crypto: Crypto by lazy { Crypto(cryptoProvider) }
    val serializer: Serializer by lazy { Serializer(serializerProvider) }

    val accountUtils: AccountUtils by lazy { AccountUtils(crypto, base58Check) }
    val base58Check: Base58Check by lazy { Base58Check(crypto) }
    val poller: Poller by lazy { Poller() }

    private val cryptoProvider: CryptoProvider by lazy {
        when (BeaconConfiguration.cryptoProvider) {
            BeaconConfiguration.CryptoProvider.LazySodium -> LazySodiumCryptoProvider()
        }
    }
    private val serializerProvider: SerializerProvider by lazy {
        when (BeaconConfiguration.serializerProvider) {
            BeaconConfiguration.SerializerProvider.Base58Check -> Base58CheckSerializerProvider(base58Check)
        }
    }

    // -- network --

    private val httpClients: MutableMap<Int, HttpClient> = mutableMapOf()
    fun httpClient(httpProvider: HttpProvider?): HttpClient {
        val httpProvider = httpProvider ?: this.httpProvider

        return httpClients.getOrPut(httpProvider.hashCode()) { HttpClient(this.httpProvider) }
    }

    private val httpProvider: HttpProvider by lazy {
        when (BeaconConfiguration.httpClientProvider) {
            BeaconConfiguration.HttpClientProvider.Ktor -> KtorHttpProvider()
        }
    }

    // -- Matrix --

    private fun matrixClient(httpProvider: HttpProvider?): MatrixClient {
        val httpClient = httpClient(httpProvider)

        return MatrixClient(
            MatrixStore(storageManager),
            MatrixNodeService(httpClient),
            MatrixUserService(httpClient),
            MatrixRoomService(httpClient),
            MatrixEventService(httpClient),
            poller,
        )
    }

    // -- Migration --

    private val migration: Migration by lazy {
        Migration(
            storageManager,
            listOf(
                MigrationFromV1_0_4(storageManager),
            )
        )
    }
}