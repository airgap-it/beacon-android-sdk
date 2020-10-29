package it.airgap.beaconsdk.data.beacon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class PermissionScope {
    @SerialName("sign") Sign,
    @SerialName("operation_request") OperationRequest,
    @SerialName("threshold") Threshold,
}