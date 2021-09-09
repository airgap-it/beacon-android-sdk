package it.airgap.beaconsdk.core.internal.utils

import android.content.Context
import it.airgap.beaconsdk.core.internal.BeaconSdk
import it.airgap.beaconsdk.core.internal.chain.ChainRegistry
import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

public val beaconSdk: BeaconSdk
    get() = BeaconSdk.instance

public val applicationContext: Context
    get() = beaconSdk.applicationContext

public val dependencyRegistry: DependencyRegistry
    get() = beaconSdk.dependencyRegistry

public val app: BeaconApplication
    get() = beaconSdk.app

public val chainRegistry: ChainRegistry
    get() = dependencyRegistry.chainRegistry