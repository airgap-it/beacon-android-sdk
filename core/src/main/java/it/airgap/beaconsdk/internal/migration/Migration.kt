package it.airgap.beaconsdk.internal.migration

import it.airgap.beaconsdk.internal.storage.StorageManager
import it.airgap.beaconsdk.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.internal.utils.logDebug

internal class Migration(private val storageManager: StorageManager, migrations: List<VersionedMigration>) {
    init {
        requireUniqueMigrations(migrations)
    }

    private fun requireUniqueMigrations(migrations: List<VersionedMigration>) {
        val duplicate = migrations
            .groupBy { it.fromVersion }
            .entries.asSequence()
            .map { it.key to it.value.size }
            .firstOrNull { it.second > 1 }

        if (duplicate != null) failWithDuplicatedMigration(duplicate.first)
    }

    private var _sdkVersion: String? = null
    private suspend fun sdkVersion(): String? =
        _sdkVersion ?: storageManager.getSdkVersion()?.also { _sdkVersion = it }

    private val migrations: List<VersionedMigration> = migrations.sortedBy { it.fromVersion }

    suspend fun migrateMatrixRelayServer(matrixNodes: List<String>): Result<Unit> =
        migrate(VersionedMigration.Target.MatrixRelayServer(matrixNodes))

    private suspend fun migrate(target: VersionedMigration.Target): Result<Unit> =
        runCatching {
            migrations
                .filter { it.targets(target) && !it.hasPerformed(target) }
                .perform(target)
        }

    private suspend fun VersionedMigration.hasPerformed(target: VersionedMigration.Target): Boolean {
        val sdkVersion = sdkVersion()
        if (sdkVersion != null && sdkVersion <= fromVersion) return true

        val performedMigrations = storageManager.getMigrations()

        val migrationIdentifier = migrationIdentifier(target)
        return performedMigrations.contains(migrationIdentifier)
    }

    private suspend fun List<VersionedMigration>.perform(target: VersionedMigration.Target) {
        val completed = mutableSetOf<String>()
        try {
            forEach { migration ->
                migration
                    .perform(target)
                    .onSuccess { logDebug(TAG, "Migration from version ${migration.fromVersion} targeted at ${target.identifier} completed.") }
                    .onFailure { logDebug(TAG, "Migration from version ${migration.fromVersion} targeted at ${target.identifier} failed.") }
                    .getOrThrow()

                val identifier = migration.migrationIdentifier(target)
                completed.add(identifier)
            }
        } finally {
            if (completed.isNotEmpty()) {
                storageManager.addMigrations(completed)
            }
        }
    }

    private fun failWithDuplicatedMigration(targetVersion: String): Nothing =
        failWithIllegalState("Duplicated migration from version $targetVersion.")

    companion object {
        private const val TAG = "Migration"
    }
}