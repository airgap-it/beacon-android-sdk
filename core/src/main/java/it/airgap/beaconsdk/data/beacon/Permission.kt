package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Granted permissions data.
 *
 * @property [accountIdentifier] The value that identifies the account which granted the permissions.
 * @property [address] The address of the account derived from its public key.
 * @property [network] The network to which the permissions apply.
 * @property [scopes] The list of granted permission types.
 * @property [senderId] The value that identifies the sender to whom the permissions were granted.
 * @property [appMetadata] The metadata describing the dApp to which the permissions were granted.
 * @property [publicKey] The public key of the account.
 * @property [connectedAt] The timestamp at which the permissions were granted.
 * @property [threshold] An optional threshold configuration.
 */
@Serializable
public data class Permission(
    public val accountIdentifier: String,
    public val address: String,
    public val network: Network,
    public val scopes: List<Scope>,
    public val senderId: String,
    public val appMetadata: AppMetadata,
    public val publicKey: String,
    public val connectedAt: Long,
    public val threshold: Threshold? = null,
) {
    public companion object {}

    /**
     * Types of permissions supported in Beacon.
     */
    @Serializable
    public enum class Scope {
        @SerialName("sign") Sign,
        @SerialName("operation_request") OperationRequest,
        @SerialName("threshold") Threshold;

        public companion object {}
    }
}