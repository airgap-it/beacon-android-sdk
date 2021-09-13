package it.airgap.beaconsdk.core.internal.migration

import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState

public abstract class Migration(migrations: List<VersionedMigration>) {
    init {
        register(migrations)
    }

    private val _migrations: MutableList<VersionedMigration> = mutableListOf()
    protected val migrations: List<VersionedMigration> get() = _migrations

    public fun register(additionalMigrations: List<VersionedMigration>) {
        _migrations.register(additionalMigrations.toList())
        requireUniqueMigrations(migrations)
    }

    public fun register(vararg additionalMigrations: VersionedMigration) {
        register(additionalMigrations.toList())
    }

    public abstract suspend fun migrate(target: Target): Result<Unit>

    private fun requireUniqueMigrations(migrations: List<VersionedMigration>) {
        val duplicate = migrations
            .groupBy { it.fromVersion }
            .entries.asSequence()
            .map { it.key to it.value.size }
            .firstOrNull { it.second > 1 }

        if (duplicate != null) failWithDuplicatedMigration(duplicate.first)
    }

    private fun failWithDuplicatedMigration(targetVersion: String): Nothing =
        failWithIllegalState("Duplicated migration from version $targetVersion.")

    private fun MutableList<VersionedMigration>.register(migrations: List<VersionedMigration>) {
        addAll(migrations)
        sortBy { it.fromVersion }
    }

    interface Target {
        val identifier: String
    }
}