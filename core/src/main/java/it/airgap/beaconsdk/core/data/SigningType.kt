package it.airgap.beaconsdk.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of signatures supported in Beacon.
 */
@Serializable
public enum class SigningType {
    /**
     * An arbitrary payload, which will be hashed before signing
     */
    @SerialName("raw") Raw,

    /**
     * Operation payload, prefixed with the "0x03" watermark
     */
    @SerialName("operation") Operation,

    /**
     * Micheline payload, prefixed with the "0x05" watermark
     */
    @SerialName("micheline") Micheline,
}