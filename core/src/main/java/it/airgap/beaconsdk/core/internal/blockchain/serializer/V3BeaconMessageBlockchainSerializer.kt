package it.airgap.beaconsdk.core.internal.blockchain.serializer

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.BlockchainV3BeaconResponseContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconRequestContent
import it.airgap.beaconsdk.core.internal.message.v3.PermissionV3BeaconResponseContent
import kotlinx.serialization.KSerializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface V3BeaconMessageBlockchainSerializer {

    // -- request --

    public val permissionRequestData: KSerializer<PermissionV3BeaconRequestContent.BlockchainData>
    public val blockchainRequestData: KSerializer<BlockchainV3BeaconRequestContent.BlockchainData>

    // -- response --

    public val permissionResponseData: KSerializer<PermissionV3BeaconResponseContent.BlockchainData>
    public val blockchainResponseData: KSerializer<BlockchainV3BeaconResponseContent.BlockchainData>
}