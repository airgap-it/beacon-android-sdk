package it.airgap.beaconsdk.core.internal.utils

import android.os.Build

internal fun sdkAtLeast(code: Int): Boolean =
    Build.VERSION.SDK_INT >= code