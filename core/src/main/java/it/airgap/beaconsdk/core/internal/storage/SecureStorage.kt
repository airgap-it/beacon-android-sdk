package it.airgap.beaconsdk.core.internal.storage

internal interface SecureStorage {
    suspend fun getSdkSecretSeed(): String?
    suspend fun setSdkSecretSeed(sdkSecretSeed: String)
}