package it.airgap.beaconsdk.core.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val beaconSdk: BeaconSdk
    get() = BeaconSdk.instance

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val applicationContext: Context
    get() = beaconSdk.applicationContext

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val dependencyRegistry: DependencyRegistry
    get() = beaconSdk.dependencyRegistry

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val app: BeaconApplication
    get() = beaconSdk.app

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val blockchainRegistry: BlockchainRegistry
    get() = dependencyRegistry.blockchainRegistry