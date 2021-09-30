package it.airgap.beaconsdk.core.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.blockchain.MockBlockchain
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class MockNetwork(
    override val name: String? = null,
    override val rpcUrl: String? = null,
) : Network() {
    @Transient
    override val blockchainIdentifier: String = MockBlockchain.IDENTIFIER

    override val type: String = TYPE

    public companion object {
        public const val TYPE: String = "mock"
    }
}