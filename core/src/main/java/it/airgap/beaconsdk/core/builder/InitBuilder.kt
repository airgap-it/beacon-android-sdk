package it.airgap.beaconsdk.core.builder

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesSecureStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import it.airgap.beaconsdk.core.internal.utils.applicationContext
import it.airgap.beaconsdk.core.internal.utils.beaconSdk
import it.airgap.beaconsdk.core.internal.utils.delegate.default
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.storage.Storage

/**
 * A base builder for the Beacon SDK's public entry points.
 *
 * @constructor Creates a builder configured with the specified application [name] and [blockchains] that will be supported in the SDK.
 */
public abstract class InitBuilder<T, Self : InitBuilder<T, Self>>(
    protected val name: String,
    protected val blockchains: List<Blockchain.Factory<*>>,
) {
    /**
     * A URL to the application's website.
     */
    public var appUrl: String? = null

    /**
     * A URL to the application's icon.
     */
    public var iconUrl: String? = null

    /**
     * Connection types that will be supported by the configured client.
     */
    protected var connections: MutableList<Connection> = mutableListOf()

    /**
     * Registers connections that should be supported by the configured client.
     */
    public fun addConnections(vararg connections: Connection): Self = self.apply {
        this.connections.addAll(connections.toList())
    }

    /**
     * An optional external implementation of [Storage]. If not provided, an internal implementation will be used.
     */
    public var storage: Storage by default { SharedPreferencesStorage.create(applicationContext) }

    /**
     * An optional external implementation of [SecureStorage]. If not provided, an internal implementation will be used.
     */
    public var secureStorage: SecureStorage by default { SharedPreferencesSecureStorage.create(
        applicationContext) }

    /**
     * Builds a new instance of [T] and initializes the SDK.
     */
    public suspend fun build(): T {
        beaconSdk.init(name, appUrl, iconUrl, blockchains, storage, secureStorage)
        return createInstance()
    }

    protected abstract suspend fun createInstance(): T

    @get:Suppress("UNCHECKED_CAST")
    private val self: Self
        get() = this as Self
}