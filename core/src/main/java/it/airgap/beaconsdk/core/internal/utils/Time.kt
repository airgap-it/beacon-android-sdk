package it.airgap.beaconsdk.core.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun currentTimestamp(): Long = System.currentTimeMillis()