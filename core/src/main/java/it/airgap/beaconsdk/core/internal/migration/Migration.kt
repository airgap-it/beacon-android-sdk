package it.airgap.beaconsdk.core.internal.migration

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Migration(migrations: List<VersionedMigration>) {
    private var _migrations: List<VersionedMigration> = emptyList()
        set(value) {
            field = value.distinctBy { it.fromVersion }.sortedBy { it.fromVersion }
        }
    protected val migrations: List<VersionedMigration> get() = _migrations

    init {
        register(migrations)
    }

    public fun register(additionalMigrations: List<VersionedMigration>) {
        _migrations = _migrations + additionalMigrations.toList()
    }

    public fun register(vararg additionalMigrations: VersionedMigration) {
        register(additionalMigrations.toList())
    }

    public abstract suspend fun migrate(target: Target): Result<Unit>

    public interface Target {
        public val identifier: String
    }
}