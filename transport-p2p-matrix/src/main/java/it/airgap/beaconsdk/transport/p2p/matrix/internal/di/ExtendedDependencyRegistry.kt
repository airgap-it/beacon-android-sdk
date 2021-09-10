package it.airgap.beaconsdk.transport.p2p.matrix.internal.di

import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.network.provider.HttpProvider
import it.airgap.beaconsdk.transport.p2p.matrix.internal.matrix.MatrixClient
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.ExtendedMigration

internal interface ExtendedDependencyRegistry : DependencyRegistry {

    // -- Matrix --

    fun matrixClient(httpProvider: HttpProvider?): MatrixClient

    // -- Migration --

    override val migration: ExtendedMigration
}