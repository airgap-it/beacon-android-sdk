package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.network.HttpClient
import it.airgap.beaconsdk.core.internal.utils.app
import it.airgap.beaconsdk.core.internal.utils.delegate.lazyWeak
import it.airgap.beaconsdk.core.network.provider.HttpClientProvider
import it.airgap.beaconsdk.transport.p2p.matrix.P2pMatrix
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pMatrixSecurity
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.event.MatrixEventService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node.MatrixNodeService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.room.MatrixRoomService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.user.MatrixUserService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.MatrixStore
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.v1_0_4.P2pMatrixMigrationFromV1_0_4
import it.airgap.beaconsdk.transport.p2p.matrix.internal.store.P2pMatrixStore
import it.airgap.beaconsdk.transport.p2p.matrix.storage.P2pMatrixStoragePlugin

internal class P2pMatrixDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- client --

    override fun p2pMatrix(storagePlugin: P2pMatrixStoragePlugin, matrixNodes: List<String>, httpClientProvider: HttpClientProvider?): P2pMatrix {
        with(storageManager) {
            if (!hasPlugin<P2pMatrixStoragePlugin>()) addPlugins(storagePlugin.extend())
        }

        val httpClient = httpClient(httpClientProvider)

        return P2pMatrix(matrixClient(httpClient), p2pMatrixStore(httpClient, matrixNodes), p2pMatrixSecurity, p2pMatrixCommunicator)
    }

    // -- P2P --

    override val p2pMatrixCommunicator: P2pMatrixCommunicator by lazyWeak { P2pMatrixCommunicator(app(beaconScope), crypto, json) }
    override val p2pMatrixSecurity: P2pMatrixSecurity by lazyWeak { P2pMatrixSecurity(app(beaconScope), crypto) }

    override fun p2pMatrixStore(httpClient: HttpClient, matrixNodes: List<String>): P2pMatrixStore =
        P2pMatrixStore(app(beaconScope), p2pMatrixCommunicator, matrixClient(httpClient), matrixNodes, storageManager, migration)

    // -- Matrix --

    private val matrixClients: MutableMap<Int, MatrixClient> = mutableMapOf()
    override fun matrixClient(httpClient: HttpClient): MatrixClient =
        matrixClients.getOrPut(httpClient.hashCode()) {
            MatrixClient(
                MatrixStore(storageManager),
                MatrixNodeService(httpClient),
                MatrixUserService(httpClient),
                MatrixRoomService(httpClient),
                MatrixEventService(httpClient),
                poller,
            )
        }

    // -- migration --

    override val migration: Migration by lazyWeak {
        dependencyRegistry.migration.apply {
            register(
                P2pMatrixMigrationFromV1_0_4(storageManager)
            )
        }
    }
}