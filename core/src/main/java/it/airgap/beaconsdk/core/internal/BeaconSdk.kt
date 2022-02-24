package it.airgap.beaconsdk.core.internal

import android.content.Context
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.CoreDependencyRegistry
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithUninitialized
import it.airgap.beaconsdk.core.internal.utils.toHexString
import it.airgap.beaconsdk.core.storage.SecureStorage
import it.airgap.beaconsdk.core.storage.Storage

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BeaconSdk(context: Context) {
    private var isInitialized: Boolean = false

    public val applicationContext: Context = context.applicationContext

    private var _dependencyRegistry: DependencyRegistry? = null
    public var dependencyRegistry: DependencyRegistry
        get() = _dependencyRegistry ?: failWithUninitialized(TAG)
        private set(value) {
            _dependencyRegistry = value
        }

    private var _app: BeaconApplication? = null
    public var app: BeaconApplication
        get() = _app ?: failWithUninitialized(TAG)
        private set(value) {
            _app = value
        }

    public val beaconId: String
        get() = app.keyPair.publicKey.toHexString().asString()

    public suspend fun init(
        partialApp: BeaconApplication.Partial,
        configuration: BeaconConfiguration,
        blockchainFactories: List<Blockchain.Factory<*>>,
        storage: Storage,
        secureStorage: SecureStorage,
    ) {
        if (isInitialized) return

        dependencyRegistry = CoreDependencyRegistry(blockchainFactories, storage, secureStorage, configuration)

        val storageManager = dependencyRegistry.storageManager
        val crypto = dependencyRegistry.crypto

        setSdkVersion(storageManager)

        app = partialApp.toFinal(loadOrGenerateKeyPair(storageManager, crypto))

        isInitialized = true
    }

    private suspend fun setSdkVersion(storageManager: StorageManager) {
        storageManager.setSdkVersion(BeaconConfiguration.sdkVersion)
    }

    private suspend fun loadOrGenerateKeyPair(storageManager: StorageManager, crypto: Crypto): KeyPair {
        val seed = storageManager.getSdkSecretSeed()
            ?: crypto.guid().getOrThrow().also { storageManager.setSdkSecretSeed(it) }

        return crypto.getKeyPairFromSeed(seed).getOrThrow()
    }

    public companion object {
        internal const val TAG = "BeaconSdk"

        @Suppress("ObjectPropertyName")
        private var _instance: BeaconSdk? = null
        public var instance: BeaconSdk
            get() = _instance ?: failWithUninitialized(TAG)
            private set(value) {
                _instance = value
            }

        internal fun create(context: Context) {
            instance = BeaconSdk(context)
        }
    }
}