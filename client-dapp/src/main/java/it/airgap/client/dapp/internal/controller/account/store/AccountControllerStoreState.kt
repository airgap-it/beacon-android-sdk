package it.airgap.client.dapp.internal.controller.account.store

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.data.Peer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AccountControllerStoreState(
    val activeAccount: Account?,
    val activePeer: Peer?,
    val defaultDestination: Origin?,
)