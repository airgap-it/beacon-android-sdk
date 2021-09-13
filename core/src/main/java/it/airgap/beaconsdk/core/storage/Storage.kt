package it.airgap.beaconsdk.core.storage

import it.airgap.beaconsdk.core.data.beacon.AppMetadata
import it.airgap.beaconsdk.core.data.beacon.Peer
import it.airgap.beaconsdk.core.data.beacon.Permission
import it.airgap.beaconsdk.core.internal.storage.decorator.DecoratedStorage

public interface Storage {

    // -- Beacon --

    public suspend fun getPeers(): List<Peer>
    public suspend fun setPeers(p2pPeers: List<Peer>)

    public suspend fun getAppMetadata(): List<AppMetadata>
    public suspend fun setAppMetadata(appMetadata: List<AppMetadata>)

    public suspend fun getPermissions(): List<Permission>
    public suspend fun setPermissions(permissions: List<Permission>)

    // -- SDK --

    public suspend fun getSdkVersion(): String?
    public suspend fun setSdkVersion(sdkVersion: String)

    public suspend fun getMigrations(): Set<String>
    public suspend fun setMigrations(migrations: Set<String>)

    public fun extend(): ExtendedStorage = DecoratedStorage(this)
}