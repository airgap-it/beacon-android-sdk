package it.airgap.beaconsdk.transport.p2p.matrix.internal.utils

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.transport.p2p.matrix.internal.di.ExtendedDependencyRegistry
import it.airgap.beaconsdk.transport.p2p.matrix.internal.di.MatrixDependencyRegistry

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry = MatrixDependencyRegistry(this)