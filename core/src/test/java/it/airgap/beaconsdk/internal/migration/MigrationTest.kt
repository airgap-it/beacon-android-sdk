package it.airgap.beaconsdk.internal.migration

import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import it.airgap.beaconsdk.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.internal.storage.MockStorage
import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.AccountUtils
import it.airgap.beaconsdk.internal.utils.failure
import it.airgap.beaconsdk.internal.utils.success
import kotlinx.coroutines.test.runBlockingTest
import mockLog
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MigrationTest {

    @MockK
    private lateinit var accountUtils: AccountUtils

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockLog()

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), accountUtils)
    }

    @Test
    fun `fails if created with duplicated versioned migrations`() {
        val version = "1"

        val versionedMigration1 = object : VersionedMigration() {
            override val fromVersion: String = version
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        }

        val versionedMigration2 = object : VersionedMigration() {
            override val fromVersion: String = version
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        }

        assertFailsWith<IllegalStateException> {
            Migration(storageManager, listOf(
                versionedMigration1,
                versionedMigration2,
            ))
        }
    }

    @Test
    fun `runs Matrix relay server migration`() {
        val matrixNodes = listOf("node1", "node2")
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Target): Boolean =
                when (target) {
                    is Target.MatrixRelayServer -> false
                    else -> true
                }
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val migration = Migration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("4")

            migration.migrateMatrixRelayServer(matrixNodes)

            val expectedInStorage = setOf(
                versionedMigration1.migrationIdentifier(target),
                versionedMigration3.migrationIdentifier(target),
            )
            val actualInStorage = storageManager.getMigrations()

            coVerify(exactly = 1) { versionedMigration1.perform(target) }
            coVerify(exactly = 0) { versionedMigration2.perform(any()) }
            coVerify(exactly = 1) { versionedMigration3.perform(target) }

            assertEquals(expectedInStorage, actualInStorage)
        }
    }

    @Test
    fun `does not run migration if SDK version precedes migration target`() {
        val matrixNodes = listOf("node1", "node2")
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Target): Boolean =
                when (target) {
                    is Target.MatrixRelayServer -> false
                    else -> true
                }
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val migration = Migration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("2")

            migration.migrateMatrixRelayServer(matrixNodes)

            val expectedInStorage = setOf(
                versionedMigration1.migrationIdentifier(target),
            )
            val actualInStorage = storageManager.getMigrations()

            coVerify(exactly = 1) { versionedMigration1.perform(target) }
            coVerify(exactly = 0) { versionedMigration2.perform(any()) }
            coVerify(exactly = 0) { versionedMigration3.perform(any()) }

            assertEquals(expectedInStorage, actualInStorage)
        }
    }

    @Test
    fun `does not run migration if SDK version equals migration target`() {
        val matrixNodes = listOf("node1", "node2")
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Target): Boolean =
                when (target) {
                    is Target.MatrixRelayServer -> false
                    else -> true
                }
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val migration = Migration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("3")

            migration.migrateMatrixRelayServer(matrixNodes)

            val expectedInStorage = setOf(
                versionedMigration1.migrationIdentifier(target),
            )
            val actualInStorage = storageManager.getMigrations()

            coVerify(exactly = 1) { versionedMigration1.perform(target) }
            coVerify(exactly = 0) { versionedMigration2.perform(any()) }
            coVerify(exactly = 0) { versionedMigration3.perform(any()) }

            assertEquals(expectedInStorage, actualInStorage)
        }
    }

    @Test
    fun `does not run already performed migration`() {
        val matrixNodes = listOf("node1", "node2")
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Target): Boolean =
                when (target) {
                    is Target.MatrixRelayServer -> false
                    else -> true
                }
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val migration = Migration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            with(storageManager) {
                setSdkVersion("4")
                setMigrations(setOf(versionedMigration1.migrationIdentifier(target)))
            }

            migration.migrateMatrixRelayServer(matrixNodes)

            val expectedInStorage = setOf(
                versionedMigration1.migrationIdentifier(target),
                versionedMigration3.migrationIdentifier(target),
            )
            val actualInStorage = storageManager.getMigrations()

            coVerify(exactly = 0) { versionedMigration1.perform(any()) }
            coVerify(exactly = 0) { versionedMigration2.perform(any()) }
            coVerify(exactly = 1) { versionedMigration3.perform(target) }

            assertEquals(expectedInStorage, actualInStorage)
        }
    }

    @Test
    fun `aborts on first migration failure`() {
        val matrixNodes = listOf("node1", "node2")
        val target = VersionedMigration.Target.MatrixRelayServer(matrixNodes)

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.failure()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Target): Boolean = true
            override suspend fun perform(target: Target): Result<Unit> = Result.success()
        })

        val migration = Migration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("4")

            migration.migrateMatrixRelayServer(matrixNodes)

            val expectedInStorage = setOf(
                versionedMigration1.migrationIdentifier(target),
            )
            val actualInStorage = storageManager.getMigrations()

            coVerify(exactly = 1) { versionedMigration1.perform(target) }
            coVerify(exactly = 1) { versionedMigration2.perform(target) }
            coVerify(exactly = 0) { versionedMigration3.perform(any()) }

            assertEquals(expectedInStorage, actualInStorage)
        }
    }
}