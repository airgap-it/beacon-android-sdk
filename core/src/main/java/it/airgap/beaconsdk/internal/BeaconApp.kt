package it.airgap.beaconsdk.internal

import android.content.Context
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.di.DependencyRegistry
import it.airgap.beaconsdk.internal.storage.SecureStorage
import it.airgap.beaconsdk.internal.storage.Storage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.asHexString
import it.airgap.beaconsdk.internal.utils.failWithUninitialized

internal class BeaconApp(context: Context) {
    var isInitialized: Boolean = false
        private set

    val applicationContext: Context = context.applicationContext

    private var _dependencyRegistry: DependencyRegistry? = null
    val dependencyRegistry: DependencyRegistry
        get() = _dependencyRegistry ?: failWithUninitialized(TAG)

    private var _appName: String? = null
    val appName: String
        get() = _appName ?: failWithUninitialized(TAG)

    var appIcon: String? = null
        private set

    var appUrl: String? = null
        private set

    private var _keyPair: KeyPair? = null
    val keyPair: KeyPair
        get() = _keyPair ?: failWithUninitialized(TAG)

    val beaconId: String
        get() = keyPair.publicKey.asHexString().asString()

    suspend fun init(
        appName: String,
        appIcon: String?,
        appUrl: String?,
        storage: Storage,
        secureStorage: SecureStorage,
    ) {
        if (isInitialized) return

        _appName = appName
        this.appIcon = appIcon
        this.appUrl = appUrl

        _dependencyRegistry = DependencyRegistry(storage, secureStorage)

        val storageManager = dependencyRegistry.storageManager
        val crypto = dependencyRegistry.crypto

        setSdkVersion(storageManager)
        loadOrGenerateKeyPair(storageManager, crypto)

        isInitialized = true
    }

    private suspend fun setSdkVersion(storageManager: StorageManager) {
        storageManager.setSdkVersion(BeaconConfiguration.sdkVersion)
    }

    private suspend fun loadOrGenerateKeyPair(storageManager: StorageManager, crypto: Crypto) {
        val seed = storageManager.getSdkSecretSeed()
            ?: crypto.guid().get().also { storageManager.setSdkSecretSeed(it) }

        _keyPair = crypto.getKeyPairFromSeed(seed).get()
    }

    companion object {
        const val TAG = "BeaconApp"

        private var _instance: BeaconApp? = null
        val instance: BeaconApp
            get() = _instance ?: failWithUninitialized(TAG)

        fun create(context: Context) {
            _instance = BeaconApp(context)
        }
    }
}