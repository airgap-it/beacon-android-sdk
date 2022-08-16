package it.airgap.beaconsdk.core.storage

import it.airgap.beaconsdk.core.scope.BeaconScope

public interface SecureStorage {
    public suspend fun getSdkSecretSeed(): String?
    public suspend fun setSdkSecretSeed(sdkSecretSeed: String)

    public fun scoped(beaconScope: BeaconScope): SecureStorage
}