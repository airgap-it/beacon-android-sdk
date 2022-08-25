package it.airgap.beaconsdk.blockchain.tezos.internal.utils

import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.data.TezosPermission
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.internal.utils.failWithAccountNetworkNotFound

internal suspend fun BeaconClient<*>.getNetworkFor(accountId: String): TezosNetwork {
    val permission = getPermissionsFor(accountId) as? TezosPermission ?: failWithAccountNetworkNotFound(accountId)
    return permission.network
}