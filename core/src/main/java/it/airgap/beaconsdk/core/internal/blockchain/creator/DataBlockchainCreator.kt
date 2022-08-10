package it.airgap.beaconsdk.core.internal.blockchain.creator

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.message.PermissionBeaconRequest
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface DataBlockchainCreator {
    public suspend fun extractIncomingPermission(request: PermissionBeaconRequest, response: PermissionBeaconResponse): Result<List<Permission>>
    public suspend fun extractOutgoingPermission(request: PermissionBeaconRequest, response: PermissionBeaconResponse): Result<List<Permission>>

    public fun extractAccounts(response: PermissionBeaconResponse): Result<List<String>>
}