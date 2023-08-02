package it.airgap.beaconsdk.blockchain.tezos.data

import kotlinx.serialization.Serializable

/**
 * Tezos threshold data.
 *
 * @property [amount] The amount of mutez that can be spent within the timeframe
 * @property [timeframe] The timeframe within which the spending will be summed up
 */
@Serializable
public data class TezosThreshold constructor(
    public val amount: String,
    public val timeframe: String
) {}