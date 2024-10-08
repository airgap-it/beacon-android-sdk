package it.airgap.beaconsdk.core.storage

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Maybe
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.decorator.DecoratedStorage
import it.airgap.beaconsdk.core.scope.BeaconScope

public interface Storage {

    // -- Beacon --

    public suspend fun getMaybePeers(): List<Maybe<Peer>>
    public suspend fun setPeers(p2pPeers: List<Peer>)

    public suspend fun getMaybeAppMetadata(): List<Maybe<AppMetadata>>
    public suspend fun setAppMetadata(appMetadata: List<AppMetadata>)

    public suspend fun getMaybePermissions(): List<Maybe<Permission>>
    public suspend fun setPermissions(permissions: List<Permission>)

    // -- SDK --

    public suspend fun getSdkVersion(): String?
    public suspend fun setSdkVersion(sdkVersion: String)

    public suspend fun getMigrations(): Set<String>
    public suspend fun setMigrations(migrations: Set<String>)

    public fun scoped(beaconScope: BeaconScope): Storage
    public fun extend(beaconConfiguration: BeaconConfiguration): ExtendedStorage = DecoratedStorage(this, beaconConfiguration)
}