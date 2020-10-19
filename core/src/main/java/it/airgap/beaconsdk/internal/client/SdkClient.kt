package it.airgap.beaconsdk.internal.client

import it.airgap.beaconsdk.internal.BeaconConfig
import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair
import it.airgap.beaconsdk.internal.storage.ExtendedStorage
import it.airgap.beaconsdk.internal.utils.asHexString

internal class SdkClient(private val storage: ExtendedStorage, private val crypto: Crypto) {
    var keyPair: KeyPair? = null
        private set

    var beaconId: String? = null
        private set

    suspend fun init() {
        setVersion()
        loadOrCreateKeyPair()
        createBeaconId()
    }

    private suspend fun setVersion() {
        storage.setSdkVersion(BeaconConfig.versionName)
    }
        
    private suspend fun loadOrCreateKeyPair() {
        val seed = storage.getSdkSecretSeed() ?: crypto.generateRandomSeed().getOrThrow().also { storage.setSdkSecretSeed(it) }

        keyPair = crypto.getKeyPairFromSeed(seed).getOrThrow()
    }

    private fun createBeaconId() {
        beaconId = keyPair?.publicKey?.asHexString()?.value(withPrefix = false)
    }

    companion object {
        const val TAG = "SdkClient"
    }
}