package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.di.findExtended
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.transport.p2p.matrix.P2pMatrix
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixSecurity
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.store.P2pMatrixStore
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- client --

    fun p2pMatrix(storagePlugin: P2pMatrixStoragePlugin, matrixNodes: List<String>, httpClientProvider: HttpClientProvider?): P2pMatrix

    // -- P2P --

    val p2pMatrixCommunicator: P2pMatrixCommunicator
    val p2pMatrixSecurity: P2pMatrixSecurity
    fun p2pMatrixStore(httpClient: HttpClient, matrixNodes: List<String>): P2pMatrixStore

    // -- Matrix --

    fun matrixClient(httpClient: HttpClient): MatrixClient
}

internal fun DependencyRegistry.extend(): ExtendedDependencyRegistry =
    if (this is ExtendedDependencyRegistry) this
    else findExtended<P2pMatrixDependencyRegistry>() ?: P2pMatrixDependencyRegistry(this).also { addExtended(it) }