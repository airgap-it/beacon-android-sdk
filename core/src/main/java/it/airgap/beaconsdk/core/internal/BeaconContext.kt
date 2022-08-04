package it.airgap.beaconsdk.core.internal

import it.airgap.beaconsdk.core.internal.data.BeaconApplication
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry

internal class BeaconContext(val app: BeaconApplication, val dependencyRegistry: DependencyRegistry)