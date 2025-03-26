package it.airgap.beaconsdk.blockchain.tezos.extension

import it.airgap.beaconsdk.blockchain.tezos.data.TezosAppMetadata
import it.airgap.beaconsdk.core.client.BeaconClient

// -- AppMetadata --

public fun <T> T.ownAppMetadata(): TezosAppMetadata where T : BeaconClient<*> =
    TezosAppMetadata(
        senderId = senderId,
        name = app.name,
        icon = app.icon,
    )