package it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.v1_0_4

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.transport.p2p.matrix.internal.BeaconP2pMatrixConfiguration
import it.airgap.beaconsdk.transport.p2p.matrix.internal.migration.MatrixMigrationTarget
import it.airgap.beaconsdk.transport.p2p.matrix.internal.storage.*
import kotlinx.coroutines.test.runBlockingTest
import mockLog
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("ClassName")
internal class P2pMatrixMigrationFromV1_0_4Test {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager
    private lateinit var migration: P2pMatrixMigrationFromV1_0_4

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockLog()

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false)).apply { addPlugins(MockP2pMatrixStoragePlugin()) }
        migration = P2pMatrixMigrationFromV1_0_4(storageManager)
    }

    @Test
    fun `targets SDK version from 1_0_4`() {
        assertEquals("1.0.4", migration.fromVersion)
    }

    @Test
    fun `targets Matrix relay server migration only`() {
        assertTrue(migration.targets(MatrixMigrationTarget.RelayServer(listOf())))
    }

    @Test
    fun `creates valid migration identifier`() {
        assertEquals(
            "from_1.0.4@matrixRelayServer",
            migration.migrationIdentifier(MatrixMigrationTarget.RelayServer(listOf())),
        )
    }

    @Test
    fun `skips if relay server is already set`() {
        val relayServer = "relayServer"
        val matrixNodes = listOf("node1", "node2")
        val target = MatrixMigrationTarget.RelayServer(matrixNodes)

        runBlockingTest {
            storageManager.setMatrixRelayServer(relayServer)

            migration.perform(target).getOrThrow()

            val expectedInStorage = relayServer
            val actualInStorage = storageManager.getMatrixRelayServer()

            assertEquals(expectedInStorage, actualInStorage)
        }
    }

    @Test
    fun `skips if list of nodes differs from defaults`() {
        val matrixNodes = listOf("node1", "node2")
        val target = MatrixMigrationTarget.RelayServer(matrixNodes)

        runBlockingTest {
            storageManager.setMatrixRelayServer(null)

            migration.perform(target).getOrThrow()

            val actualInStorage = storageManager.getMatrixRelayServer()

            assertNull(actualInStorage)
        }
    }

    @Test
    fun `skips if there is no active Matrix connection to maintain`() {
        val matrixNodes = BeaconP2pMatrixConfiguration.defaultNodes
        val target = MatrixMigrationTarget.RelayServer(matrixNodes)

        runBlockingTest {
            with(storageManager) {
                setMatrixRelayServer(null)
                setMatrixSyncToken(null)
                setMatrixRooms(emptyList())
            }

            migration.perform(target).getOrThrow()

            val actualInStorage = storageManager.getMatrixRelayServer()

            assertNull(actualInStorage)
        }
    }

    @Test
    fun `sets old default node if migration is needed`() {
        val matrixNodes = BeaconP2pMatrixConfiguration.defaultNodes
        val target = MatrixMigrationTarget.RelayServer(matrixNodes)

        runBlockingTest {
            with(storageManager) {
                setMatrixRelayServer(null)
                setMatrixSyncToken("token")
            }

            migration.perform(target).getOrThrow()

            val expectedInStorage = P2pMatrixMigrationFromV1_0_4.V1_0_4_DEFAULT_NODE
            val actualInStorage = storageManager.getMatrixRelayServer()

            assertEquals(expectedInStorage, actualInStorage)
        }
    }
}