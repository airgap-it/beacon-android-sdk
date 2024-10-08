package it.airgap.beaconsdk.client.dapp.internal.storage

import it.airgap.beaconsdk.client.dapp.internal.storage.decorator.DecoratedDAppClientStorage
import it.airgap.beaconsdk.client.dapp.storage.DAppClientStorage
import it.airgap.beaconsdk.client.dapp.storage.ExtendedDAppClientStorage
import it.airgap.beaconsdk.client.dapp.data.PairedAccount
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage

public class MockDAppClientStorage : DAppClientStorage, Storage by MockStorage() {
    private var activeAccount: PairedAccount? = null
    private var activePeer: String? = null

    override suspend fun getActiveAccount(): PairedAccount? = activeAccount
    override suspend fun setActiveAccount(account: PairedAccount?) {
        this.activeAccount = account
    }

    override suspend fun getActivePeer(): String? = activePeer
    override suspend fun setActivePeer(peerId: String?) {
        this.activePeer = peerId
    }

    override fun scoped(beaconScope: BeaconScope): DAppClientStorage = this
    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStorage = DecoratedDAppClientStorage(this, beaconConfiguration)
}