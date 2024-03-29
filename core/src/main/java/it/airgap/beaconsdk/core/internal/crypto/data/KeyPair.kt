package it.airgap.beaconsdk.core.internal.crypto.data

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class KeyPair(public val privateKey: ByteArray, public val publicKey: ByteArray)