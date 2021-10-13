package it.airgap.beaconsdk.core.internal.compat

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.internal.utils.failWithIllegalState

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Compat<T : VersionedCompat> {
    private val allVersioned: MutableList<T> = mutableListOf()
    public val versioned: T
        get() = allVersioned.lastOrNull { it.withVersion < BeaconConfiguration.sdkVersion }
            ?: allVersioned.last()

    public fun register(additionalVersioned: List<T>) {
        allVersioned.register(additionalVersioned)
        requireUniqueVersioned()
    }

    public fun register(vararg additionalVersioned: T) {
        register(additionalVersioned.toList())
    }

    private fun requireUniqueVersioned() {
        val duplicate = allVersioned
            .groupBy { it.withVersion }
            .entries.asSequence()
            .map { it.key to it.value.size }
            .firstOrNull { it.second > 1 }

        if (duplicate != null) failWithDuplicatedMigration(duplicate.first)
    }

    private fun failWithDuplicatedMigration(targetVersion: String): Nothing =
        failWithIllegalState("Duplicated compat with version $targetVersion.")

    private fun MutableList<T>.register(versioned: List<T>) {
        addAll(versioned)
        sortBy { it.withVersion }
    }
}