package it.airgap.beaconsdk.core.internal.migration

import it.airgap.beaconsdk.core.internal.utils.success

public abstract class VersionedMigration {
    abstract val fromVersion: String

    fun migrationIdentifier(target: Migration.Target): String =
        "from_$fromVersion@${target.identifier}"

    abstract fun targets(target: Migration.Target): Boolean

    abstract suspend fun perform(target: Migration.Target): Result<Unit>

    protected fun skip(): Result<Unit> = Result.success()
}