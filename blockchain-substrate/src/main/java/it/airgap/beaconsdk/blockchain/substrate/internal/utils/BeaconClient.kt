package it.airgap.beaconsdk.blockchain.substrate.internal.utils

import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.data.SubstratePermission
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.internal.utils.failWithAccountNetworkNotFound

internal suspend fun BeaconClient<*>.getNetworkFor(accountId: String): SubstrateNetwork {
    val permission = getPermissionsFor(accountId) as? SubstratePermission
    return permission?.account?.network ?: failWithAccountNetworkNotFound(accountId)
}