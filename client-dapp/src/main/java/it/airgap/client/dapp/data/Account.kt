package it.airgap.client.dapp.data

import it.airgap.beaconsdk.core.data.Origin
import kotlinx.serialization.Serializable

@Serializable
public data class Account internal constructor(
    public val accountId: String,
    public val peerId: String,
    public val origin: Origin,
    public val publicKey: String,
    public val connectedAt: Long,
)
