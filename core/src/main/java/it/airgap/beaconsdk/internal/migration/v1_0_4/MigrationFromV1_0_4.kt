package it.airgap.beaconsdk.internal.migration.v1_0_4

import it.airgap.beaconsdk.internal.BeaconConfiguration
import it.airgap.beaconsdk.internal.migration.VersionedMigration
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.runCatchingFlat
import it.airgap.beaconsdk.internal.utils.success

@Suppress("ClassName")
internal class MigrationFromV1_0_4(private val storageManager: StorageManager) : VersionedMigration() {
    override val fromVersion: String = "1.0.4"

    override fun targets(target: Target): Boolean =
        when (target) {
            is Target.MatrixRelayServer -> true
            else -> false
        }

    override suspend fun perform(target: Target): Result<Unit> =
        with (target) {
            when (this) {
                is Target.MatrixRelayServer -> migrateMatrixRelayServer(matrixNodes)
                else -> skip()
            }
        }

    private suspend fun migrateMatrixRelayServer(matrixNodes: List<String>): Result<Unit> =
        runCatchingFlat {
            val savedRelayServer = storageManager.getMatrixRelayServer()
            if (savedRelayServer != null) {
                /* a relay server is set, no need to perform the migration */
                return skip()
            }

            if (matrixNodes != BeaconConfiguration.defaultRelayServers) {
                /* the migration can't be performed if the list of nodes differs from the default list */
                return skip()
            }

            val matrixSyncToken = storageManager.getMatrixSyncToken()
            val matrixRooms = storageManager.getMatrixRooms()

            if (matrixSyncToken == null && matrixRooms.isEmpty()) {
                /* no connection that needs to be maintained */
                return skip()
            }

            /* use the old default node to avoid peers from losing their relay server */
            storageManager.setMatrixRelayServer(V1_0_4_DEFAULT_NODE)

            Result.success()
        }

    companion object {
        const val V1_0_4_DEFAULT_NODE = "matrix.papers.tech"
    }
}