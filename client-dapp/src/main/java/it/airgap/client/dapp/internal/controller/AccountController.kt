package it.airgap.client.dapp.internal.controller

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.internal.storage.StorageManager
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import it.airgap.client.dapp.internal.storage.setActivePeerId

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AccountController(private val storageManager: StorageManager) {

    public suspend fun onPairingResponse(pairingResponse: PairingResponse): Result<Unit> =
        runCatching {
            storageManager.setActivePeerId(pairingResponse.toPeer().id)
        }
}