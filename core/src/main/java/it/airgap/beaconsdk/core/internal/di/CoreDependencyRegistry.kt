package it.airgap.beaconsdk.core.internal.di

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.data.P2P
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.provider.CryptoProvider
import it.airgap.beaconsdk.core.internal.crypto.provider.LazySodiumCryptoProvider
import it.airgap.beaconsdk.core.internal.migration.CoreMigration
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.network.provider.KtorHttpProvider
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.serializer.provider.Base58CheckSerializerProvider
import it.airgap.beaconsdk.core.internal.serializer.provider.SerializerProvider
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.transport.p2p.P2pTransport
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.Poller
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.storage.Storage

internal class CoreDependencyRegistry(blockchainFactories: List<Blockchain.Factory<*>>, storage: Storage, secureStorage: SecureStorage) : DependencyRegistry {

    // -- storage --

    override val storageManager: StorageManager by lazy { StorageManager(storage, secureStorage, identifierCreator) }

    // -- blockchain --

    override val blockchainRegistry: BlockchainRegistry by lazy {
        val blockchainFactories = blockchainFactories
            .map { it.identifier to { it.create(this) } }
            .toMap()

        BlockchainRegistry(blockchainFactories)
    }

    // -- controller --

    override val messageController: MessageController by lazy { MessageController(blockchainRegistry, storageManager, identifierCreator) }

    override fun connectionController(connections: List<Connection>): ConnectionController {
            val transports = connections.map { transport(it) }

            return ConnectionController(transports, serializer)
        }

    // -- transport --

    override fun transport(connection: Connection): Transport =
        when (connection) {
            is P2P -> P2pTransport(storageManager, connection.client.create(this))
        }

    // -- utils --

    override val crypto: Crypto by lazy { Crypto(cryptoProvider) }
    override val serializer: Serializer by lazy { Serializer(serializerProvider) }

    override val identifierCreator: IdentifierCreator by lazy { IdentifierCreator(crypto, base58Check) }
    override val base58Check: Base58Check by lazy { Base58Check(crypto) }
    override val poller: Poller by lazy { Poller() }

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
    override fun httpClient(httpProvider: HttpProvider?): HttpClient {
        val httpProvider = httpProvider ?: this.httpProvider

        return httpClients.getOrPut(httpProvider.hashCode()) { HttpClient(this.httpProvider) }
    }

    private val httpProvider: HttpProvider by lazy {
        when (BeaconConfiguration.httpClientProvider) {
            BeaconConfiguration.HttpClientProvider.Ktor -> KtorHttpProvider()
        }
    }

    // -- migration --

    override val migration: Migration by lazy {
        CoreMigration(
            storageManager,
            listOf(),
        )
    }
}