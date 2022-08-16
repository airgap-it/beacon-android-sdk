package it.airgap.beaconsdk.client.dapp.internal.controller.account.store

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.data.Peer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface AccountControllerStoreAction

internal data class OnPeerPaired(val peer: Peer) : AccountControllerStoreAction
internal data class OnPeerRemoved(val peer: Peer) : AccountControllerStoreAction
internal object ResetActivePeer : AccountControllerStoreAction

internal data class OnNewActiveAccount(val account: Account) : AccountControllerStoreAction
internal object ResetActiveAccount : AccountControllerStoreAction

internal object HardReset : AccountControllerStoreAction