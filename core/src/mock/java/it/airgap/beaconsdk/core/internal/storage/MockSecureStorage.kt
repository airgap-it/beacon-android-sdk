package it.airgap.beaconsdk.core.internal.storage

internal class MockSecureStorage : SecureStorage {
    private var sdkSecretSeed: String? = null

    override suspend fun getSdkSecretSeed(): String? = sdkSecretSeed
    override suspend fun setSdkSecretSeed(sdkSecretSeed: String) {
        this.sdkSecretSeed = sdkSecretSeed
    }
}