package it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.v1_0_4

import it.airgap.beaconsdk.core.internal.migration.Migration
import it.airgap.beaconsdk.core.internal.migration.VersionedMigration
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.runCatchingFlat
import it.airgap.beaconsdk.core.internal.utils.success
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.MatrixMigrationTarget
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.getMatrixRelayServer
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.getMatrixRooms
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.getMatrixSyncToken
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.setMatrixRelayServer

@Suppress("ClassName")
internal class P2pMatrixMigrationFromV1_0_4(private val storageManager: StorageManager) : VersionedMigration() {
    override val fromVersion: String = "1.0.4"

    override fun targets(target: Migration.Target): Boolean =
        when (target) {
            is MatrixMigrationTarget.RelayServer -> true
            else -> false
        }

    override suspend fun perform(target: Migration.Target): Result<Unit> =
        with (target) {
            when (this) {
                is MatrixMigrationTarget.RelayServer -> migrateMatrixRelayServer(nodes)
                else -> skip()
            }
        }

    private suspend fun migrateMatrixRelayServer(nodes: List<String>): Result<Unit> =
        runCatchingFlat {
            val savedRelayServer = storageManager.getMatrixRelayServer()
            if (savedRelayServer != null) {
                /* a relay server is set, no need to perform the migration */
                return skip()
            }

            if (nodes != BeaconP2pMatrixConfiguration.defaultNodes) {
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