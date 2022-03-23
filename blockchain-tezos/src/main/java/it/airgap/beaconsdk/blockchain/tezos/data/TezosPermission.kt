package it.airgap.beaconsdk.blockchain.tezos.data

import it.airgap.beaconsdk.blockchain.tezos.Tezos
import it.airgap.beaconsdk.core.data.Permission
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Tezos permission data.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the permission is valid.
 * @property [accountId] The value that identifies the account which granted the permissions.
 * @property [senderId] The value that identifies the sender to whom the permissions were granted.
 * @property [connectedAt] The timestamp at which the permissions were granted.
 * @property [address] The address of the account derived from its public key.
 * @property [publicKey] The public key of the account.
 * @property [network] The network to which the permission apply.
 * @property [appMetadata] The metadata describing the dApp to which the permissions were granted.
 * @property [scopes] The list of granted permission types.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
public data class TezosPermission internal constructor(
    @JsonNames("accountIdentifier") override val accountId: String,
    override val senderId: String,
    override val connectedAt: Long,
    public val address: String,
    public val publicKey: String,
    public val network: TezosNetwork,
    public val appMetadata: TezosAppMetadata,
    public val scopes: List<Scope>,
) : Permission() {
    @EncodeDefault
    override val blockchainIdentifier: String = Tezos.IDENTIFIER

    /**
     * Types of Tezos permission supported in Beacon.
     */
    @Serializable
    public enum class Scope {
        @SerialName("sign") Sign,
        @SerialName("operation_request") OperationRequest;

        public companion object {}
    }

    public companion object {}
}