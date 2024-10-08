package it.airgap.beaconsdk.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.blockchain.substrate.Substrate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Metadata describing a Substrate dApp.
 *
 * @property [blockchainIdentifier] The unique name of the blockchain on which the dApp operates.
 * @property [senderId] The value that identifies the dApp.
 * @property [name] The name of the dApp.
 * @property [icon] An optional URL for the dApp icon.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
public data class SubstrateAppMetadata(
    override val senderId: String,
    override val name: String,
    override val icon: String? = null,
) : AppMetadata() {
    @EncodeDefault
    override val blockchainIdentifier: String = Substrate.IDENTIFIER
}