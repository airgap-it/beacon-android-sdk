package it.airgap.beaconsdk.core.internal.crypto.data

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SessionKeyPair(public val rx: ByteArray, public val tx: ByteArray)