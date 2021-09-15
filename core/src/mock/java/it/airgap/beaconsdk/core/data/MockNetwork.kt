package it.airgap.beaconsdk.core.data

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class MockNetwork(
    override val name: String? = null,
    override val rpcUrl: String? = null,
) : Network() {
    override val identifier: String = IDENTIFIER

    public companion object {
        public const val IDENTIFIER: String = "mock"
    }
}