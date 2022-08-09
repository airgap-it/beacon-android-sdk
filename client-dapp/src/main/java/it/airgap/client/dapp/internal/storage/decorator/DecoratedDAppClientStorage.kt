package it.airgap.client.dapp.internal.storage.decorator

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Maybe
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.decorator.DecoratedStorage
import it.airgap.beaconsdk.core.internal.utils.app
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.ExtendedStorage
import it.airgap.client.dapp.storage.DAppClientStorage
import it.airgap.client.dapp.storage.ExtendedDAppClientStorage

internal class DecoratedDAppClientStorage(
    private val plugin: DAppClientStorage,
    private val beaconConfiguration: BeaconConfiguration,
) : ExtendedDAppClientStorage, DAppClientStorage by plugin, ExtendedStorage by DecoratedStorage(plugin, beaconConfiguration) {

    override suspend fun removeActiveAccount() {
        setActiveAccount(null)
    }

    override suspend fun removeActivePeer() {
        setActivePeer(null)
    }

    override suspend fun getMaybePeers(): List<Maybe<Peer>> = plugin.getMaybePeers()
    override suspend fun setPeers(p2pPeers: List<Peer>) = plugin.setPeers(p2pPeers)

    override suspend fun getMaybeAppMetadata(): List<Maybe<AppMetadata>> = plugin.getMaybeAppMetadata()
    override suspend fun setAppMetadata(appMetadata: List<AppMetadata>) = plugin.setAppMetadata(appMetadata)

    override suspend fun getMaybePermissions(): List<Maybe<Permission>> = plugin.getMaybePermissions()
    override suspend fun setPermissions(permissions: List<Permission>) = plugin.setPermissions(permissions)

    override suspend fun getSdkVersion(): String? = plugin.getSdkVersion()
    override suspend fun setSdkVersion(sdkVersion: String) = plugin.setSdkVersion(sdkVersion)

    override suspend fun getMigrations(): Set<String> = plugin.getMigrations()
    override suspend fun setMigrations(migrations: Set<String>) = plugin.setMigrations(migrations)

    override fun scoped(beaconScope: BeaconScope): ExtendedDAppClientStorage = DecoratedDAppClientStorage(plugin.scoped(beaconScope), beaconConfiguration)
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStorage = this
}