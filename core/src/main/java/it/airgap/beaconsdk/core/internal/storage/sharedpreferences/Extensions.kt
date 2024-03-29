package it.airgap.beaconsdk.core.internal.storage.sharedpreferences

import it.airgap.beaconsdk.core.data.AppMetadata
import it.airgap.beaconsdk.core.data.Maybe
import it.airgap.beaconsdk.core.data.Peer
import it.airgap.beaconsdk.core.data.Permission
import it.airgap.beaconsdk.core.exception.BlockchainNotFoundException
import it.airgap.beaconsdk.core.internal.BeaconConfiguration
import it.airgap.beaconsdk.core.storage.Storage

public suspend fun Storage.getPeers(beaconConfiguration: BeaconConfiguration): List<Peer> = getMaybePeers().values(beaconConfiguration)
public suspend fun Storage.getAppMetadata(beaconConfiguration: BeaconConfiguration): List<AppMetadata> = getMaybeAppMetadata().values(beaconConfiguration)
public suspend fun Storage.getPermissions(beaconConfiguration: BeaconConfiguration): List<Permission> = getMaybePermissions().values(beaconConfiguration)

private fun <T> List<Maybe<T>>.values(beaconConfiguration: BeaconConfiguration): List<T> =
    mapNotNull {
        when (it) {
            is Maybe.Some -> it.value
            is Maybe.None -> null
            is Maybe.NoneWithError -> it.discardOrThrow(beaconConfiguration)
        }
    }

private fun <T> Maybe.NoneWithError<T>.discardOrThrow(beaconConfiguration: BeaconConfiguration): T? = with (beaconConfiguration) {
    when {
        exception is BlockchainNotFoundException && ignoreUnsupportedBlockchains -> null
        else -> throw exception
    }
}