package it.airgap.beaconsdk.internal

import android.content.Context
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.di.DependencyRegistry
import it.airgap.beaconsdk.internal.storage.Storage
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

    private var _keyPair: KeyPair? = null
    val keyPair: KeyPair
        get() = _keyPair ?: failWithUninitialized(TAG)

    val beaconId: String
        get() = keyPair.publicKey.asHexString().value()

    suspend fun init(appName: String, matrixNodes: List<String>, storage: Storage) {
        if (isInitialized) return

        _appName = appName
        _dependencyRegistry = DependencyRegistry(matrixNodes, storage)

        val extendedStorage = dependencyRegistry.extendedStorage
        val crypto = dependencyRegistry.crypto

        setSdkVersion(extendedStorage)
        loadOrGenerateKeyPair(extendedStorage, crypto)

        isInitialized = true
    }

    private suspend fun setSdkVersion(storage: Storage) {
        storage.setSdkVersion(BeaconConfig.sdkVersion)
    }

    private suspend fun loadOrGenerateKeyPair(storage: Storage, crypto: Crypto) {
        val seed = storage.getSdkSecretSeed()
            ?: crypto.generateRandomSeed().value().also { storage.setSdkSecretSeed(it) }

        _keyPair = crypto.getKeyPairFromSeed(seed).value()
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