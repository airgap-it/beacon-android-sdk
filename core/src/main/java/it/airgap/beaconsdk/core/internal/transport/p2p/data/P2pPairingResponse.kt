package it.airgap.beaconsdk.core.internal.transport.p2p.data

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
public data class P2pPairingResponse(
    val id: String,
    val type: String,
    val name: String,
    val version: String,
    val publicKey: String,
    val relayServer: String,
    val icon: String? = null,
    val appUrl: String? = null,
)