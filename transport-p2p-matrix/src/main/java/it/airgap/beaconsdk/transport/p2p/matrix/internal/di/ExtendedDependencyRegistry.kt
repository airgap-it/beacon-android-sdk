package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pCrypto
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.store.P2pStore

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- P2P --

    val p2pCommunicator: P2pCommunicator
    fun p2pStore(httpProvider: HttpProvider?, matrixNodes: List<String>): P2pStore
    val p2pCrypto: P2pCrypto

    // -- Matrix --

    fun matrixClient(httpProvider: HttpProvider?): MatrixClient
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else MatrixDependencyRegistry(this)