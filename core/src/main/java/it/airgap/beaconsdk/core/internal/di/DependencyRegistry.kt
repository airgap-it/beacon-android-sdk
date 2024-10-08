package it.airgap.beaconsdk.core.internal.di

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.compat.Compat
import it.airgap.beaconsdk.core.internal.compat.VersionedCompat
import it.airgap.beaconsdk.core.internal.controller.connection.ConnectionController
import it.airgap.beaconsdk.core.internal.controller.message.MessageController
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.serializer.Serializer
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.transport.Transport
import it.airgap.beaconsdk.core.internal.utils.Base58
import it.airgap.beaconsdk.core.internal.utils.Base58Check
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.Poller
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.core.scope.BeaconScope
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

public interface DependencyRegistry {

    public val beaconScope: BeaconScope

    // -- extended --

    public val extended: Map<String, DependencyRegistry>
    public fun addExtended(extended: DependencyRegistry)
    public fun <T : DependencyRegistry> findExtended(targetClass: KClass<T>): T?

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
    public val base58: Base58
    public val base58Check: Base58Check
    public val poller: Poller

    // -- network --

    public val json: Json
    public fun httpClient(httpClientProvider: HttpClientProvider?): HttpClient

    @Deprecated(
        "Use httpClient(HttpClientProvider?) instead.",
        replaceWith = ReplaceWith("httpClient(httpProvider)"),
        level = DeprecationLevel.WARNING,
    )
    public fun httpClient(httpProvider: HttpProvider): HttpClient

    // -- migration --

    public val migration: Migration

    // -- compat --

    public val compat: Compat<VersionedCompat>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : DependencyRegistry> DependencyRegistry.findExtended(): T? = findExtended(T::class)