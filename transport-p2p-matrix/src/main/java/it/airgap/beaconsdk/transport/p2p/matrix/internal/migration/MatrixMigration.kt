package it.airgap.beaconsdk.transport.p2p.matrix.internal.migration

import it.airgap.beaconsdk.core.internal.migration.Migration

internal class MatrixMigration(migration: Migration) : ExtendedMigration, Migration by migration {
    override suspend fun migrateMatrixRelayServer(matrixNodes: List<String>): Result<Unit> =
        migrate(Target.RelayServer(matrixNodes))

    sealed interface Target : Migration.Target {

        data class RelayServer(val nodes: List<String>) : Target {
            override val identifier: String = IDENTIFIER

            companion object {
                const val IDENTIFIER = "matrixRelayServer"
            }
        }
    }
}