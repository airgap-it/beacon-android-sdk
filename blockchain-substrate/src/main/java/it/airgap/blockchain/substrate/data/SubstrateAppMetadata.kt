package it.airgap.blockchain.substrate.data

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.blockchain.substrate.Substrate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

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