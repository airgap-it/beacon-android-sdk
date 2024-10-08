package it.airgap.beaconsdk.core.internal.transport.p2p.data

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class P2pMessage(val publicKey: String, val content: String) {
    public companion object {}
}