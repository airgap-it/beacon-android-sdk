package it.airgap.beaconsdk.core.internal.migration

import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.logDebug

internal class CoreMigration(private val storageManager: StorageManager, migrations: List<VersionedMigration>) : Migration(migrations) {
    private var _sdkVersion: String? = null
    private suspend fun sdkVersion(): String? =
        _sdkVersion ?: storageManager.getSdkVersion()?.also { _sdkVersion = it }

    override suspend fun migrate(target: Target): Result<Unit> =
        runCatching {
            migrations
                .filter { it.targets(target) && !it.hasPerformed(target) }
                .perform(target)
        }

    private suspend fun VersionedMigration.hasPerformed(target: Target): Boolean {
        val sdkVersion = sdkVersion()
        if (sdkVersion != null && sdkVersion <= fromVersion) return true

        val performedMigrations = storageManager.getMigrations()

        val migrationIdentifier = migrationIdentifier(target)
        return performedMigrations.contains(migrationIdentifier)
    }

    private suspend fun List<VersionedMigration>.perform(target: Target) {
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

    companion object {
        private const val TAG = "Migration"
    }
}