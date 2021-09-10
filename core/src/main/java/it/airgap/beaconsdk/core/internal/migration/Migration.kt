package it.airgap.beaconsdk.core.internal.migration

public interface Migration {
    public fun register(additionalMigrations: List<VersionedMigration>)
    public fun register(vararg additionalMigrations: VersionedMigration)

    public suspend fun migrate(target: Target): Result<Unit>

    interface Target {
        val identifier: String
    }
}