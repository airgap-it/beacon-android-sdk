package it.airgap.beaconsdk.core.data

import kotlinx.serialization.Serializable

/**
 * Metadata describing a dApp.
 *
 * @property [senderId] The value that identifies the dApp.
 * @property [name] The name of the dApp.
 * @property [icon] An optional URL for the dApp icon.
 */
@Serializable
public data class AppMetadata(
    public val senderId: String,
    public val name: String,
    public val icon: String? = null,
) {
    public companion object {}
}