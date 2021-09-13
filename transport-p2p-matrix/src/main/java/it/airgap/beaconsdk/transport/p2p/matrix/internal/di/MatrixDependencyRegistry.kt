package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.event.MatrixEventService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.node.MatrixNodeService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.room.MatrixRoomService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.network.user.MatrixUserService
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.store.MatrixStore
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.v1_0_4.MigrationFromV1_0_4

internal class MatrixDependencyRegistry(dependencyRegistry: DependencyRegistry) : ExtendedDependencyRegistry, DependencyRegistry by dependencyRegistry {

    // -- Matrix --

    override fun matrixClient(httpProvider: HttpProvider?): MatrixClient {
        val httpClient = httpClient(httpProvider)

        return MatrixClient(
            MatrixStore(storageManager),
            MatrixNodeService(httpClient),
            MatrixUserService(httpClient),
            MatrixRoomService(httpClient),
            MatrixEventService(httpClient),
            poller,
        )
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