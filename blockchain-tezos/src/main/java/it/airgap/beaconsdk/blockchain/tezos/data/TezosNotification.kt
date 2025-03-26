package it.airgap.beaconsdk.blockchain.tezos.data

import kotlinx.serialization.Serializable

/**
 * Tezos notification data.
 *
 * @property [version] The notification version
 * @property [apiUrl] The api url the notification refers to
 * @property [token] The generated token
 */
@Serializable
public data class TezosNotification constructor(
    public val version: Int,
    public val apiUrl: String,
    public val token: String
) {}