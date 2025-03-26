package it.airgap.beaconsdk.blockchain.substrate.extensions

import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAppMetadata
import it.airgap.beaconsdk.core.client.BeaconClient

// -- AppMetadata --

public fun <T> T.ownAppMetadata(): SubstrateAppMetadata where T : BeaconClient<*> =
    SubstrateAppMetadata(
        senderId = senderId,
        name = app.name,
        icon = app.icon,
    )