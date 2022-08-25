package it.airgap.beaconsdk.client.dapp.internal.storage.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import it.airgap.beaconsdk.client.dapp.data.PairedAccount
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesBaseStorage
import it.airgap.beaconsdk.core.internal.storage.sharedpreferences.SharedPreferencesStorage
import it.airgap.beaconsdk.core.scope.BeaconScope
import it.airgap.beaconsdk.core.storage.Storage
import it.airgap.beaconsdk.client.dapp.internal.storage.decorator.DecoratedDAppClientStorage
import it.airgap.beaconsdk.client.dapp.storage.DAppClientStorage
import it.airgap.beaconsdk.client.dapp.storage.ExtendedDAppClientStorage

internal class SharedPreferencesDAppClientStorage(
    private val storage: Storage,
    sharedPreferences: SharedPreferences,
    beaconScope: BeaconScope = BeaconScope.Global,
) : DAppClientStorage, SharedPreferencesBaseStorage(beaconScope, sharedPreferences), Storage by storage {
    override suspend fun getActiveAccount(): PairedAccount? =
        sharedPreferences.getSerializable(Key.ActiveAccount.scoped(), null, beaconScope)

    override suspend fun setActiveAccount(account: PairedAccount?) {
        sharedPreferences.putSerializable(Key.ActiveAccount.scoped(), account, beaconScope)
    }

    override suspend fun getActivePeer(): String? =
        sharedPreferences.getString(Key.ActivePeerId.scoped(), null)

    override suspend fun setActivePeer(peerId: String?) {
        sharedPreferences.putString(Key.ActivePeerId.scoped(), peerId)
    }

    override fun scoped(beaconScope: BeaconScope): DAppClientStorage =
        if (beaconScope == this.beaconScope) this
        else SharedPreferencesDAppClientStorage(storage.scoped(beaconScope), sharedPreferences, beaconScope)

    override fun extend(beaconConfiguration: BeaconConfiguration): ExtendedDAppClientStorage =
        DecoratedDAppClientStorage(this, beaconConfiguration)

    private enum class Key(override val value: String) : SharedPreferencesBaseStorage.Key {
        ActiveAccount("dappActiveAccount"),
        ActivePeerId("dappActivePeerId"),
    }
}

internal fun SharedPreferencesDAppClientStorage(context: Context): SharedPreferencesDAppClientStorage {
    val sharedPreferences = context.getSharedPreferences(BeaconConfiguration.STORAGE_NAME, Context.MODE_PRIVATE)
    val sharedPreferencesCoreStorage = SharedPreferencesStorage(context)

    return SharedPreferencesDAppClientStorage(sharedPreferencesCoreStorage, sharedPreferences)
}