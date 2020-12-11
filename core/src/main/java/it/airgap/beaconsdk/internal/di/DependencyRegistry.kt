package it.airgap.beaconsdk.internal.di

import it.airgap.beaconsdk.data.beacon.Connection
import it.airgap.beaconsdk.data.beacon.P2P
import it.airgap.beaconsdk.internal.BeaconApp
import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.controller.ConnectionController
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.internal.crypto.provider.LazySodiumCryptoProvider
import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.provider.HttpClientProvider
import it.airgap.beaconsdk.internal.network.provider.KtorHttpClientProvider
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
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixEventService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixRoomService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixUserService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.MatrixStore
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCommunicationUtils
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pServerUtils
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Base58Check
import it.airgap.beaconsdk.internal.utils.Poller
import it.airgap.beaconsdk.internal.utils.asHexString

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

    private val transports: MutableMap<Connection.Type, Transport> = mutableMapOf()

    fun transport(connection: Connection): Transport =
        transports.getOrPut(connection.type) {
            when (connection) {
                is P2P -> {
                    val appName = BeaconApp.instance.appName
                    val keyPair = BeaconApp.instance.keyPair

                    val replicationCount = BeaconConfiguration.P2P_REPLICATION_COUNT

                    val p2pServerUtils = P2pServerUtils(crypto, connection.nodes)
                    val p2pCommunicationUtils = P2pCommunicationUtils(crypto)

                    val matrixClients = (0 until replicationCount).map {
                        val relayServer = p2pServerUtils.getRelayServer(keyPair.publicKey, it.asHexString())
                        matrixClient("https://$relayServer/${BeaconConfiguration.MATRIX_CLIENT_API.trimStart('/')}")
                    }

                    val client = P2pClient(
                        appName,
                        p2pServerUtils,
                        p2pCommunicationUtils,
                        matrixClients,
                        replicationCount,
                        crypto,
                        keyPair
                    )

                    return P2pTransport(storageManager, client)
                }
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

    private val httpClients: MutableMap<String, HttpClient> = mutableMapOf()

    fun httpClient(baseUrl: String): HttpClient =
        httpClients.getOrPut(baseUrl) { HttpClient(httpClientProvider(baseUrl)) }

    private fun httpClientProvider(baseUrl: String): HttpClientProvider =
        when (BeaconConfiguration.httpClientProvider) {
            BeaconConfiguration.HttpClientProvider.Ktor -> KtorHttpClientProvider(baseUrl)
        }

    // -- Matrix --

    private fun matrixClient(baseUrl: String): MatrixClient =
        MatrixClient(
            matrixClientStore,
            matrixUserService(baseUrl),
            matrixRoomService(baseUrl),
            matrixEventService(baseUrl),
            poller,
        )

    private val matrixClientStore: MatrixStore by lazy { MatrixStore(storageManager) }

    private fun matrixUserService(baseUrl: String): MatrixUserService = MatrixUserService(httpClient(baseUrl))
    private fun matrixRoomService(baseUrl: String): MatrixRoomService = MatrixRoomService(httpClient(baseUrl))
    private fun matrixEventService(baseUrl: String): MatrixEventService = MatrixEventService(httpClient(baseUrl))
}