package it.airgap.beaconsdk.core.internal.di

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.controller.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import it.airgap.beaconsdk.core.internal.utils.Poller
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface DependencyRegistry {

    // -- extensions --

    public val extensions: Map<String, DependencyRegistry>
    public fun addExtension(extension: DependencyRegistry)

    // -- storage --

    public val storageManager: StorageManager

    // -- blockchain --

    public val blockchainRegistry: BlockchainRegistry

    // -- controller --

    public val messageController: MessageController
    public fun connectionController(connections: List<Connection>): ConnectionController

    // -- transport --

    public fun transport(connection: Connection): Transport

    // -- utils --

    public val crypto: Crypto
    public val serializer: Serializer

    public val identifierCreator: IdentifierCreator
    public val base58Check: Base58Check
    public val poller: Poller

    // -- network --

    public fun httpClient(httpProvider: HttpProvider?): HttpClient

    // -- migration --

    public val migration: Migration
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : DependencyRegistry> DependencyRegistry.findExtension(): T? {
    val key = T::class.simpleName ?: return null
    return extensions[key] as? T
}