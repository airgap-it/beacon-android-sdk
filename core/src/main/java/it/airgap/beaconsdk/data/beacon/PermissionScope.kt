package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of permissions supported in Beacon.
 */
@Serializable
public enum class PermissionScope {
    @SerialName("sign") Sign,
    @SerialName("operation_request") OperationRequest,
    @SerialName("threshold") Threshold;

    public companion object {}
}