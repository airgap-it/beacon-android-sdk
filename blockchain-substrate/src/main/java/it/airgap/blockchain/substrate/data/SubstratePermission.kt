package it.airgap.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.data.Threshold
import it.airgap.blockchain.substrate.Substrate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class SubstratePermission(
    override val accountIdentifier: String,
    override val address: String,
    override val senderId: String,
    override val appMetadata: AppMetadata,
    override val publicKey: String,
    override val connectedAt: Long,
    public val network: SubstrateNetwork,
    public val scopes: List<Scope>,
    override val threshold: Threshold?
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
}