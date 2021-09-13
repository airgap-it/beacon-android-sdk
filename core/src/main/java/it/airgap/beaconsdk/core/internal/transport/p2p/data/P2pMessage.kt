package it.airgap.beaconsdk.core.internal.transport.p2p.data

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
public data class P2pMessage(val id: String, val content: String) {
    public companion object {}
}