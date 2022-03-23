package it.airgap.beaconsdk.core.builder

import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.data.Connection
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
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
 * @constructor Creates a builder configured with the specified application [name].
 */
public abstract class InitBuilder<T, Self : InitBuilder<T, Self>>(protected val name: String) {
    // -- app --

    /**
     * A URL to the application's website.
     */
    public var appUrl: String? = null

    /**
     * A URL to the application's icon.
     */
    public var iconUrl: String? = null

    // -- blockchains --

    /**
     * Blockchains that will be supported in the SDK.
     */
    protected val blockchains: MutableList<Blockchain.Factory<*>> = mutableListOf()

    /**
     * Registers blockchains that should be supported by the configured client.
     */
    public fun support(vararg blockchains: Blockchain.Factory<*>): Self = self.apply {
        this.blockchains.addAll(blockchains.toList())
    }

    // -- connections --

    /**
     * Connection types that will be supported by the configured client.
     */
    protected val connections: MutableList<Connection> = mutableListOf()

    /**
     * Registers connections that should be supported by the configured client.
     */
    public fun use(vararg connections: Connection): Self = self.apply {
        this.connections.addAll(connections.toList())
    }

    // -- storage --

    /**
     * An optional external implementation of [Storage]. If not provided, an internal implementation will be used.
     */
    public var storage: Storage by default { SharedPreferencesStorage.create(applicationContext) }

    /**
     * An optional external implementation of [SecureStorage]. If not provided, an internal implementation will be used.
     */
    public var secureStorage: SecureStorage by default { SharedPreferencesSecureStorage.create(applicationContext) }

    // -- configuration --

    /**
     * Specifies whether the client should fail when handling data of a blockchain that it does not support or silently ignore it.
     */
    public var ignoreUnsupportedBlockchains: Boolean = false

    /**
     * Builds a new instance of [T] and initializes the SDK.
     */
    public suspend fun build(): T {
        val configuration = BeaconConfiguration(ignoreUnsupportedBlockchains)
        val partialApp = BeaconApplication.Partial(name, iconUrl, appUrl)

        beaconSdk.init(partialApp, configuration, blockchains, storage, secureStorage)

        return createInstance(configuration)
    }

    protected abstract suspend fun createInstance(configuration: BeaconConfiguration): T

    @get:Suppress("UNCHECKED_CAST")
    private val self: Self
        get() = this as Self
}