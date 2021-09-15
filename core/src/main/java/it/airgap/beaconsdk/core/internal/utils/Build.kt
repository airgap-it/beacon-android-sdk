package it.airgap.beaconsdk.core.internal.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ChecksSdkIntAtLeast(parameter = 0)
public fun sdkAtLeast(code: Int): Boolean =
    Build.VERSION.SDK_INT >= code