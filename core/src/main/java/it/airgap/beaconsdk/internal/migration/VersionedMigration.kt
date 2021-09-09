package it.airgap.beaconsdk.internal.migration

import it.airgap.beaconsdk.internal.utils.success

internal abstract class VersionedMigration {
    abstract val fromVersion: String

    fun migrationIdentifier(target: Target): String =
        "from_$fromVersion@${target.identifier}"

    abstract fun targets(target: Target): Boolean

    abstract suspend fun perform(target: Target): Result<Unit>

    protected fun skip(): Result<Unit> = Result.success()

    sealed interface Target {
        val identifier: String

        data class MatrixRelayServer(val matrixNodes: List<String>) : Target {
            override val identifier: String = IDENTIFIER

            companion object {
                const val IDENTIFIER = "matrixRelayServer"
            }
        }
    }
}