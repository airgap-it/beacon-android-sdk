package it.airgap.beaconsdk.core.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class MockAppMetadata(
    override val senderId: String,
    override val name: String,
    override val icon: String? = null,
) : AppMetadata() {
    @Required
    override val blockchainIdentifier: String = MockBlockchain.IDENTIFIER
}