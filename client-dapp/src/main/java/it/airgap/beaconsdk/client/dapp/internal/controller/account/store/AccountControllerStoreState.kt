package it.airgap.beaconsdk.client.dapp.internal.controller.account.store

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.client.dapp.data.PairedAccount
import it.airgap.beaconsdk.core.data.Peer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AccountControllerStoreState(
    val activeAccount: PairedAccount?,
    val activePeer: Peer?,
)
