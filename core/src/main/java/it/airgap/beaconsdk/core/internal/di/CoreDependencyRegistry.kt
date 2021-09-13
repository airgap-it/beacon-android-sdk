package it.airgap.beaconsdk.core.internal.di

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.beacon.Connection
import it.airgap.beaconsdk.core.data.beacon.P2P
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.chain.Chain
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
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
import it.airgap.beaconsdk.core.internal.storage.SecureStorage
import it.airgap.beaconsdk.core.internal.storage.Storage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.storage.StoragePlugin
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.transport.p2p.P2pTransport
import it.airgap.beaconsdk.core.internal.utils.AccountUtils
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import it.airgap.beaconsdk.core.internal.utils.Poller
import it.airgap.beaconsdk.core.network.provider.HttpProvider

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CoreDependencyRegistry(chainFactories: List<Chain.Factory<*>>, storage: Storage, secureStorage: SecureStorage, storagePlugins: List<StoragePlugin>) : DependencyRegistry {

    // -- storage --
    override val storageManager: StorageManager by lazy { StorageManager(storage, secureStorage, storagePlugins, accountUtils) }

    // -- chain --

    override val chainRegistry: ChainRegistry by lazy {
        val chainFactories = chainFactories
            .map { it.identifier to { it.create(this) } }
            .toMap()

        ChainRegistry(chainFactories)
    }

    // -- controller --

    override val messageController: MessageController by lazy { MessageController(chainRegistry, storageManager, accountUtils) }

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

    override val accountUtils: AccountUtils by lazy { AccountUtils(crypto, base58Check) }
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