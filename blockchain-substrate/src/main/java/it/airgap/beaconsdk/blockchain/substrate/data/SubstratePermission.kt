package it.airgap.beaconsdk.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
 * @property [scopes] The list of granted permission types.
 * @property [account] The account to which the permission apply.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
public data class SubstratePermission(
    override val accountId: String,
    override val senderId: String,
    override val connectedAt: Long,
    public val appMetadata: SubstrateAppMetadata,
    public val scopes: List<Scope>,
    public val account: SubstrateAccount,
) : Permission() {
    @EncodeDefault
    override val blockchainIdentifier: String = Substrate.IDENTIFIER

    /**
     * Types of Substrate permission supported in Beacon.
     */
    @Serializable
    public enum class Scope {
        @SerialName("transfer") Transfer,
        @SerialName("sign_payload_json") SignPayloadJson,
        @SerialName("sign_payload_raw") SignPayloadRaw;

        public companion object {}
    }

    public companion object {}
}