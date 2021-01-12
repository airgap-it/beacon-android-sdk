package it.airgap.beaconsdk.internal.storage

internal interface SecureStorage {
    suspend fun getSdkSecretSeed(): String?
    suspend fun setSdkSecretSeed(sdkSecretSeed: String)
}