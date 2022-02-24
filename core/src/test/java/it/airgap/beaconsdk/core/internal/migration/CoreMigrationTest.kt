package it.airgap.beaconsdk.core.internal.migration

import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.storage.MockSecureStorage
import it.airgap.beaconsdk.core.internal.storage.MockStorage
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator
import it.airgap.beaconsdk.core.internal.utils.failure
import it.airgap.beaconsdk.core.internal.utils.success
import kotlinx.coroutines.test.runBlockingTest
import mockLog
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class CoreMigrationTest {

    @MockK
    private lateinit var identifierCreator: IdentifierCreator

    private lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockLog()

        storageManager = StorageManager(MockStorage(), MockSecureStorage(), identifierCreator, BeaconConfiguration(ignoreUnsupportedBlockchains = false))
    }

    @Test
    fun `takes first migration if created with duplicated versioned migrations`() {
        val version = "1"
        val target = MockTarget

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = version
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = version
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val migration = CoreMigration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
        ))

        runBlockingTest {
            migration.migrate(target)

            coVerify(exactly = 1) { versionedMigration1.perform(target) }
            coVerify(exactly = 0) { versionedMigration2.perform(any()) }
        }
    }

    @Test
    fun `runs migration`() {
        val target = MockTarget

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Migration.Target): Boolean =
                when (target) {
                    is MockTarget -> false
                    else -> true
                }
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val migration = CoreMigration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("4")

            migration.migrate(target)

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
    fun `runs migration on dynamically registered VersionedMigration`() {
        val target = MockTarget

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Migration.Target): Boolean =
                when (target) {
                    is MockTarget -> false
                    else -> true
                }
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val migration = CoreMigration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("4")

            migration.register(versionedMigration3)
            migration.migrate(target)

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
        val target = MockTarget

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Migration.Target): Boolean =
                when (target) {
                    is MockTarget -> false
                    else -> true
                }
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val migration = CoreMigration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("2")

            migration.migrate(target)

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
        val target = MockTarget

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Migration.Target): Boolean =
                when (target) {
                    is MockTarget -> false
                    else -> true
                }
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val migration = CoreMigration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("3")

            migration.migrate(target)

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
        val target = MockTarget

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Migration.Target): Boolean =
                when (target) {
                    is MockTarget -> false
                    else -> true
                }
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val migration = CoreMigration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            with(storageManager) {
                setSdkVersion("4")
                setMigrations(setOf(versionedMigration1.migrationIdentifier(target)))
            }

            migration.migrate(target)

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
        val target = MockTarget

        val versionedMigration1 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "1"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val versionedMigration2 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "2"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.failure()
        })

        val versionedMigration3 = spyk(object : VersionedMigration() {
            override val fromVersion: String = "3"
            override fun targets(target: Migration.Target): Boolean = true
            override suspend fun perform(target: Migration.Target): Result<Unit> = Result.success()
        })

        val migration = CoreMigration(storageManager, listOf(
            versionedMigration1,
            versionedMigration2,
            versionedMigration3,
        ))

        runBlockingTest {
            storageManager.setSdkVersion("4")

            migration.migrate(target)

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

    private object MockTarget : Migration.Target {
        override val identifier: String = "mockTarget"
    }
}