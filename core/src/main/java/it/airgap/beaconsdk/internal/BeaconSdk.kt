package it.airgap.beaconsdk.internal

import android.content.Context
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.data.BeaconApplication
import it.airgap.beaconsdk.internal.di.DependencyRegistry
import it.airgap.beaconsdk.internal.storage.SecureStorage
import it.airgap.beaconsdk.internal.storage.Storage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.failWithUninitialized
import it.airgap.beaconsdk.internal.utils.toHexString

internal class BeaconSdk(context: Context) {
    var isInitialized: Boolean = false
        private set

    val applicationContext: Context = context.applicationContext

    private var _dependencyRegistry: DependencyRegistry? = null
    val dependencyRegistry: DependencyRegistry
        get() = _dependencyRegistry ?: failWithUninitialized(TAG)

    private var _app: BeaconApplication? = null
    val app: BeaconApplication
        get() = _app ?: failWithUninitialized(TAG)

    val beaconId: String
        get() = app.keyPair.publicKey.toHexString().asString()

    suspend fun init(
        appName: String,
        appIcon: String?,
        appUrl: String?,
        storage: Storage,
        secureStorage: SecureStorage,
    ) {
        if (isInitialized) return

        _dependencyRegistry = DependencyRegistry(storage, secureStorage)

        val storageManager = dependencyRegistry.storageManager
        val crypto = dependencyRegistry.crypto

        setSdkVersion(storageManager)

        _app = BeaconApplication(
            loadOrGenerateKeyPair(storageManager, crypto),
            appName,
            appIcon,
            appUrl,
        )

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

    companion object {
        const val TAG = "BeaconSdk"

        @Suppress("ObjectPropertyName")
        private var _instance: BeaconSdk? = null
        val instance: BeaconSdk
            get() = _instance ?: failWithUninitialized(TAG)

        fun create(context: Context) {
            _instance = BeaconSdk(context)
        }
    }
}