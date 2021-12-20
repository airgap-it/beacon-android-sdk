package it.airgap.beaconsdk.blockchain.tezos.data

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.blockchain.tezos.internal.compat.TezosCompat
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.data.Threshold
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tezos permission data.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the permission is valid.
 * @property [accountId] The value that identifies the account which granted the permissions.
 * @property [address] The address of the account derived from its public key.
 * @property [senderId] The value that identifies the sender to whom the permissions were granted.
 * @property [appMetadata] The metadata describing the dApp to which the permissions were granted.
 * @property [publicKey] The public key of the account.
 * @property [connectedAt] The timestamp at which the permissions were granted.
 * @property [network] The network to which the permission apply.
 * @property [scopes] The list of granted permission types.
 * @property [threshold] An optional threshold configuration.
 */
@Serializable(with = TezosPermission.Serializer::class)
public data class TezosPermission internal constructor(
    override val accountId: String,
    public val address: String,
    override val senderId: String,
    override val appMetadata: AppMetadata,
    public val publicKey: String,
    override val connectedAt: Long,
    public val network: TezosNetwork,
    public val scopes: List<Scope>,
    override val threshold: Threshold? = null,
) : Permission() {
    override val blockchainIdentifier: String = Tezos.IDENTIFIER

    /**
     * Types of Tezos permission supported in Beacon.
     */
    @Serializable
    public enum class Scope {
        @SerialName("sign") Sign,
        @SerialName("operation_request") OperationRequest,
        @SerialName("threshold") Threshold;

        public companion object {}
    }

    internal object Serializer : KSerializer<TezosPermission> by TezosCompat.versioned.tezosPermissionSerializer
}