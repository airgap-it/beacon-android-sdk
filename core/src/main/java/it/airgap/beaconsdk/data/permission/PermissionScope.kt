package it.airgap.beaconsdk.data.permission

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PermissionScope {
    @SerialName("sign") Sign,
    @SerialName("operation_request") OperationRequest,
    @SerialName("threshold") Threshold
}