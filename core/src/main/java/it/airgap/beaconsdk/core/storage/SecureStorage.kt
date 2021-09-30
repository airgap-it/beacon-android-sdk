package it.airgap.beaconsdk.core.storage

public interface SecureStorage {
    public suspend fun getSdkSecretSeed(): String?
    public suspend fun setSdkSecretSeed(sdkSecretSeed: String)
}