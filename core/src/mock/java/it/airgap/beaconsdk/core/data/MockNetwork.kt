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
    override val blockchainIdentifier: String = MockBlockchain.IDENTIFIER
    override val identifier: String
        get() = mutableListOf(TYPE).apply {
            name?.let { add("name:$it") }
            rpcUrl?.let { add("rpc:$it") }
        }.joinToString("-")


    public companion object {
        public const val TYPE: String = "mock"
    }
}