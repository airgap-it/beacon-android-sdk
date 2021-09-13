package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.utils.app
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pCommunicator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.P2pCrypto
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.event.MatrixEventService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node.MatrixNodeService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.room.MatrixRoomService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.user.MatrixUserService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.MatrixStore
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.v1_0_4.MigrationFromV1_0_4
import it.airgap.beaconsdk.transport.p2p.matrix.internal.store.P2pStore

internal class MatrixDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- P2P --

    override val p2pCommunicator: P2pCommunicator by lazy { P2pCommunicator(app, crypto) }

    override fun p2pStore(httpProvider: HttpProvider?, matrixNodes: List<String>): P2pStore =
        P2pStore(app, p2pCommunicator, matrixClient(httpProvider), matrixNodes, storageManager, migration)

    override val p2pCrypto: P2pCrypto by lazy { P2pCrypto(app, crypto) }

    // -- Matrix --

    private val matrixClients: MutableMap<Int, MatrixClient> = mutableMapOf()
    override fun matrixClient(httpProvider: HttpProvider?): MatrixClient {
        val httpClient = httpClient(httpProvider)

        return matrixClients.getOrPut(httpClient.hashCode()) {
            MatrixClient(
                MatrixStore(storageManager),
                MatrixNodeService(httpClient),
                MatrixUserService(httpClient),
                MatrixRoomService(httpClient),
                MatrixEventService(httpClient),
                poller,
            )
        }
    }

    // -- migration --

    override val migration: Migration by lazy {
        dependencyRegistry.migration.apply {
            register(
                MigrationFromV1_0_4(storageManager)
            )
        }
    }
}