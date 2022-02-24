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
import it.airgap.beaconsdk.core.internal.utils.Base58
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.Poller
import it.airgap.beaconsdk.core.internal.utils.delegate.lazyWeak
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.storage.Storage
import kotlin.reflect.KClass

internal class CoreDependencyRegistry(
    blockchainFactories: List<Blockchain.Factory<*>>,
    storage: Storage,
    secureStorage: SecureStorage,
    private val beaconConfiguration: BeaconConfiguration,
) : DependencyRegistry {

    // -- extended --

    private val _extended: MutableMap<String, DependencyRegistry> = mutableMapOf()
    override val extended: Map<String, DependencyRegistry>
        get() = _extended

    override fun addExtended(extended: DependencyRegistry) {
        val key = extended::class.simpleName ?: return
        _extended[key] = extended
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : DependencyRegistry> findExtended(targetClass: KClass<T>): T? {
        val key = targetClass.simpleName ?: return null
        return extended[key] as T?
    }

    // -- storage --

    override val storageManager: StorageManager by lazyWeak { StorageManager(storage, secureStorage, identifierCreator, beaconConfiguration) }

    // -- blockchain --

    override val blockchainRegistry: BlockchainRegistry by lazyWeak {
        val blockchainFactories = blockchainFactories
            .map { it.identifier to { it.create(this) } }
            .toMap()

        BlockchainRegistry(blockchainFactories)
    }

    // -- controller --

    override val messageController: MessageController by lazyWeak { MessageController(blockchainRegistry, storageManager, identifierCreator) }

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

    override val crypto: Crypto by lazyWeak { Crypto(cryptoProvider) }
    override val serializer: Serializer by lazyWeak { Serializer(serializerProvider) }

    override val identifierCreator: IdentifierCreator by lazyWeak { IdentifierCreator(crypto, base58Check) }
    override val base58: Base58 by lazyWeak { Base58() }
    override val base58Check: Base58Check by lazyWeak { Base58Check(base58, crypto) }
    override val poller: Poller by lazyWeak { Poller() }

    private val cryptoProvider: CryptoProvider by lazyWeak {
        when (BeaconConfiguration.cryptoProvider) {
            BeaconConfiguration.CryptoProvider.LazySodium -> LazySodiumCryptoProvider()
        }
    }
    private val serializerProvider: SerializerProvider by lazyWeak {
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

    private val httpProvider: HttpProvider by lazyWeak {
        when (BeaconConfiguration.httpClientProvider) {
            BeaconConfiguration.HttpClientProvider.Ktor -> KtorHttpProvider()
        }
    }

    // -- migration --

    override val migration: Migration by lazyWeak {
        CoreMigration(
            storageManager,
            listOf(),
        )
    }
}