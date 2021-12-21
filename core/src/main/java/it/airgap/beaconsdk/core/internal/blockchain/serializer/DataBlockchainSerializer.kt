package it.airgap.beaconsdk.core.internal.blockchain.serializer

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.data.Network
import it.airgap.beaconsdk.core.data.Permission
import kotlinx.serialization.KSerializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface DataBlockchainSerializer {
    public val network: KSerializer<Network>
    public val permission: KSerializer<Permission>
    public val appMetadata: KSerializer<AppMetadata>
    public val error: KSerializer<BeaconError>
}