package it.airgap.beaconsdk.internal.di

import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.client.ConnectionClient
import it.airgap.beaconsdk.internal.client.SdkClient
import it.airgap.beaconsdk.internal.controller.MessageController
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.internal.crypto.provider.LazySodiumCryptoProvider
import it.airgap.beaconsdk.internal.transport.p2p.matrix.MatrixClient
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixUserService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.store.MatrixStore
import it.airgap.beaconsdk.internal.network.HttpClient
import it.airgap.beaconsdk.internal.network.HttpPoller
import it.airgap.beaconsdk.internal.network.provider.HttpClientProvider
import it.airgap.beaconsdk.internal.network.provider.KtorHttpClientProvider
import it.airgap.beaconsdk.internal.protocol.ProtocolRegistry
import it.airgap.beaconsdk.internal.serializer.Serializer
import it.airgap.beaconsdk.internal.serializer.provider.Base58CheckSerializerProvider
import it.airgap.beaconsdk.internal.serializer.provider.SerializerProvider
import it.airgap.beaconsdk.internal.transport.p2p.P2pTransport
import it.airgap.beaconsdk.internal.transport.Transport
import it.airgap.beaconsdk.internal.transport.p2p.P2pClient
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixEventService
import it.airgap.beaconsdk.internal.transport.p2p.matrix.network.MatrixRoomService
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pCommunicationUtils
import it.airgap.beaconsdk.internal.transport.p2p.utils.P2pServerUtils
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.Base58Check
import it.airgap.beaconsdk.internal.utils.asHexString
import it.airgap.beaconsdk.internal.utils.failWithUninitialized
import it.airgap.beaconsdk.storage.BeaconStorage

internal class DependencyRegistry(
    private val appName: String,
    private val matrixNodes: List<String>,
    storage: BeaconStorage,
) {
    val extendedStorage: ExtendedStorage by lazy { ExtendedStorage(storage) }

    val sdkClient: SdkClient by lazy { SdkClient(extendedStorage, crypto) }

    val messageController: MessageController by lazy { MessageController(protocolRegistry, extendedStorage, accountUtils) }
    val protocolRegistry: ProtocolRegistry by lazy { ProtocolRegistry(crypto, base58Check) }

    val httpPoller: HttpPoller by lazy { HttpPoller() }

    val crypto: Crypto by lazy { Crypto(cryptoProvider) }
    val serializer: Serializer by lazy { Serializer(serializerProvider) }

    val accountUtils: AccountUtils by lazy { AccountUtils(crypto, base58Check) }
    val base58Check: Base58Check by lazy { Base58Check(crypto) }

    fun connectionController(transportType: Transport.Type): ConnectionClient =
        connectionControllers.getOrPut(transportType) {
            val transport = this.transport(transportType)

            return ConnectionClient(transport, serializer)
        }

    fun transport(type: Transport.Type): Transport =
        transports.getOrPut(type) {
            when (type) {
                Transport.Type.P2P -> {
                    val keyPair = sdkClient.keyPair ?: failWithUninitialized(SdkClient.TAG)
                    val replicationCount = BeaconConfig.p2pReplicationCount
                    val client = P2pClient(
                        appName,
                        p2pServerUtils,
                        p2pCommunicationUtils,
                        matrixClients(keyPair, replicationCount),
                        replicationCount,
                        crypto,
                        keyPair
                    )

                    return P2pTransport(appName, extendedStorage, client)
                }
            }
        }

    fun matrixClient(baseUrl: String): MatrixClient =
        MatrixClient(
            matrixClientStore,
            matrixUserService(baseUrl),
            matrixRoomService(baseUrl),
            matrixEventService(baseUrl),
            httpPoller,
        )

    fun httpClient(baseUrl: String): HttpClient =
        httpClients.getOrPut(baseUrl) { HttpClient(httpClientProvider(baseUrl)) }

    private val connectionControllers: MutableMap<Transport.Type, ConnectionClient> = mutableMapOf()
    private val transports: MutableMap<Transport.Type, Transport> = mutableMapOf()
    private val httpClients: MutableMap<String, HttpClient> = mutableMapOf()

    private val p2pServerUtils: P2pServerUtils by lazy { P2pServerUtils(crypto, matrixNodes) }
    private val p2pCommunicationUtils: P2pCommunicationUtils by lazy { P2pCommunicationUtils(crypto) }

    private val cryptoProvider: CryptoProvider by lazy {
        when (BeaconConfig.cryptoProvider) {
            BeaconConfig.CryptoProvider.LazySodium -> LazySodiumCryptoProvider()
        }
    }
    private val serializerProvider: SerializerProvider by lazy {
        when (BeaconConfig.serializerProvider) {
            BeaconConfig.SerializerProvider.Base58Check -> Base58CheckSerializerProvider(base58Check)
        }
    }

    private fun httpClientProvider(baseUrl: String): HttpClientProvider =
        when (BeaconConfig.httpClientProvider) {
            BeaconConfig.HttpClientProvider.Ktor -> KtorHttpClientProvider(baseUrl)
        }

    private fun matrixClients(keyPair: KeyPair, replicationCount: Int): List<MatrixClient> =
        (0 until replicationCount).map {
            val relayServer = p2pServerUtils.getRelayServer(keyPair.publicKey, it.asHexString())
            matrixClient("https://$relayServer/${BeaconConfig.matrixClientApi}")
        }

    private val matrixClientStore: MatrixStore by lazy { MatrixStore(extendedStorage) }

    private fun matrixUserService(baseUrl: String): MatrixUserService = MatrixUserService(httpClient(baseUrl))
    private fun matrixRoomService(baseUrl: String): MatrixRoomService = MatrixRoomService(httpClient(baseUrl))
    private fun matrixEventService(baseUrl: String): MatrixEventService = MatrixEventService(httpClient(baseUrl))

    companion object {
        const val TAG = "GlobalServiceLocator"
    }
}