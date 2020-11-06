package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.Serializable

/**
 * Threshold configuration.
 *
 * The threshold is not enforced by Beacon on the dApp side. It has to be enforced in the wallet.
 *
 * @property [amount] The amount of mutez that can be spend within the timeframe.
 * @property [timeframe] The timeframe within which the spending will be summed up.
 */
@Serializable
public data class Threshold(
    public val amount: String,
    public val timeframe: String,
) {
    public companion object {}
}