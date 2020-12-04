package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of signatures supported in Beacon.
 */
@Serializable
public enum class SigningType {
    @SerialName("raw") Raw
}