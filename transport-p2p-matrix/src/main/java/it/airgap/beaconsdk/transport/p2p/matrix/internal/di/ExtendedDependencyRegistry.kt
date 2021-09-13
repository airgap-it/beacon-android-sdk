package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- Matrix --

    fun matrixClient(httpProvider: HttpProvider?): MatrixClient
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry = MatrixDependencyRegistry(this)