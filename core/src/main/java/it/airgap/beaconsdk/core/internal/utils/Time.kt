package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun currentTimestamp(): Long = System.currentTimeMillis()