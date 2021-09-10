package it.airgap.beaconsdk.core.internal.migration

import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState
import it.airgap.beaconsdk.core.internal.utils.logDebug

public class CoreMigration(private val storageManager: StorageManager, migrations: List<VersionedMigration>) : Migration {
    init {
        register(migrations)
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

    private val migrations: MutableList<VersionedMigration> = mutableListOf()

    override fun register(additionalMigrations: List<VersionedMigration>) {
        migrations.register(additionalMigrations.toList())
        requireUniqueMigrations(migrations)
    }

    override fun register(vararg additionalMigrations: VersionedMigration) {
        register(additionalMigrations.toList())
    }

    override suspend fun migrate(target: Migration.Target): Result<Unit> =
        runCatching {
            migrations
                .filter { it.targets(target) && !it.hasPerformed(target) }
                .perform(target)
        }

    private suspend fun VersionedMigration.hasPerformed(target: Migration.Target): Boolean {
        val sdkVersion = sdkVersion()
        if (sdkVersion != null && sdkVersion <= fromVersion) return true

        val performedMigrations = storageManager.getMigrations()

        val migrationIdentifier = migrationIdentifier(target)
        return performedMigrations.contains(migrationIdentifier)
    }

    private suspend fun List<VersionedMigration>.perform(target: Migration.Target) {
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

    private fun MutableList<VersionedMigration>.register(migrations: List<VersionedMigration>) {
        addAll(migrations)
        sortBy { it.fromVersion }
    }

    private fun failWithDuplicatedMigration(targetVersion: String): Nothing =
        failWithIllegalState("Duplicated migration from version $targetVersion.")

    companion object {
        private const val TAG = "Migration"
    }
}