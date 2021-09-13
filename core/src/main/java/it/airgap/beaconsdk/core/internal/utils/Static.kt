package it.airgap.beaconsdk.core.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

@get:RestrictTo(RestrictTo.Scope.LIBRARY)
public val beaconSdk: BeaconSdk
    get() = BeaconSdk.instance

@get:RestrictTo(RestrictTo.Scope.LIBRARY)
public val applicationContext: Context
    get() = beaconSdk.applicationContext

@get:RestrictTo(RestrictTo.Scope.LIBRARY)
public val dependencyRegistry: DependencyRegistry
    get() = beaconSdk.dependencyRegistry
@get:RestrictTo(RestrictTo.Scope.LIBRARY)
public val app: BeaconApplication
    get() = beaconSdk.app
@get:RestrictTo(RestrictTo.Scope.LIBRARY)
public val chainRegistry: ChainRegistry
    get() = dependencyRegistry.chainRegistry