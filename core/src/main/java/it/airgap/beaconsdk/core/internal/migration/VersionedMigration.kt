package it.airgap.beaconsdk.core.internal.migration

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.utils.success

@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class VersionedMigration {
    public abstract val fromVersion: String

    public fun migrationIdentifier(target: Migration.Target): String =
        "from_$fromVersion@${target.identifier}"

    public abstract fun targets(target: Migration.Target): Boolean

    public abstract suspend fun perform(target: Migration.Target): Result<Unit>

    protected fun skip(): Result<Unit> = Result.success()
}