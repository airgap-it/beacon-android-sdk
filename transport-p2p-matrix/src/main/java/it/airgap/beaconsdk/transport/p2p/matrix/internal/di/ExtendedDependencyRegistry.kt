package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixSecurity
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.store.P2pMatrixStore

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- P2P --

    val p2pMatrixCommunicator: P2pMatrixCommunicator
    val p2pMatrixSecurity: P2pMatrixSecurity
    fun p2pMatrixStore(httpProvider: HttpProvider?, matrixNodes: List<String>): P2pMatrixStore

    // -- Matrix --

    fun matrixClient(httpProvider: HttpProvider?): MatrixClient
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else P2pMatrixDependencyRegistry(this)