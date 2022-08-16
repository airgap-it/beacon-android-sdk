package it.airgap.beaconsdk.core.internal.storage

import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.SecureStorage

public class MockSecureStorage : SecureStorage {
    private var sdkSecretSeed: String? = null

    override suspend fun getSdkSecretSeed(): String? = sdkSecretSeed
    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) {
        this.sdkSecretSeed = sdkSecretSeed
    }

    override fun scoped(beaconScope: BeaconScope): SecureStorage = this
}