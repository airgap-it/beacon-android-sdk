package it.airgap.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.blockchain.substrate.Substrate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Substrate permission data.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the permission is valid.
 * @property [accountId] The value that identifies the account which granted the permissions.
 * @property [senderId] The value that identifies the sender to whom the permissions were granted.
 * @property [appMetadata] The metadata describing the dApp to which the permissions were granted.
 * @property [connectedAt] The timestamp at which the permissions were granted.
 * @property [network] The network to which the permission apply.
 * @property [scopes] The list of granted permission types.
 */
@Serializable
public data class SubstratePermission(
    override val accountId: String,
    override val senderId: String,
    override val appMetadata: AppMetadata,
    override val connectedAt: Long,
    public val network: SubstrateNetwork,
    public val scopes: List<Scope>,
) : Permission() {
    override val blockchainIdentifier: String = Substrate.IDENTIFIER

    /**
     * Types of Substrate permission supported in Beacon.
     */
    @Serializable
    public enum class Scope {
        @SerialName("transfer") Transfer,
        @SerialName("sign_raw") SignRaw,
        @SerialName("sign_string") SignString;

        public companion object {}
    }

    public companion object {}
}