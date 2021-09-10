package it.airgap.beaconsdk.transport.p2p.matrix.internal.migration

import it.airgap.beaconsdk.core.internal.migration.Migration

public interface ExtendedMigration : Migration {
    suspend fun migrateMatrixRelayServer(matrixNodes: List<String>): Result<Unit>
}