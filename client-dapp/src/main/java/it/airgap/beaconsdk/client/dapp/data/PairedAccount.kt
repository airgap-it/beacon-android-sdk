package it.airgap.beaconsdk.client.dapp.data

import it.airgap.beaconsdk.core.data.Account
import kotlinx.serialization.Serializable

@Serializable
public data class PairedAccount(
    public val account: Account,
    public val peerId: String,
)
