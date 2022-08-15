package it.airgap.beaconsdk.client.dapp.internal.controller.account

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.*
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import it.airgap.beaconsdk.client.dapp.internal.controller.account.store.*
import it.airgap.beaconsdk.client.dapp.internal.controller.account.store.HardReset
import it.airgap.beaconsdk.client.dapp.internal.controller.account.store.OnNewActiveAccount
import it.airgap.beaconsdk.client.dapp.internal.controller.account.store.OnPeerPaired
import it.airgap.beaconsdk.client.dapp.internal.controller.account.store.ResetActiveAccount

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AccountController(private val store: AccountControllerStore, private val blockchainRegistry: BlockchainRegistry) {

    public suspend fun getRequestDestination(): Connection.Id? =
        store.state().getOrThrow().activePeer?.toConnectionId()

    public suspend fun getActiveAccountId(): String? =
        store.state().getOrThrow().activeAccount?.accountId

    public suspend fun clearActiveAccountId() {
        store.intent(ResetActiveAccount)
    }

    public suspend fun clearAll() {
        store.intent(HardReset)
    }

    public suspend fun onPairingResponse(pairingResponse: PairingResponse): Result<Unit> =
        runCatching {
            store.intent(OnPeerPaired(pairingResponse.toPeer()))
        }

    public suspend fun onPermissionResponse(origin: Connection.Id, response: PermissionBeaconResponse): Result<Unit> =
        runCatching {
            val blockchain = blockchainRegistry.get(response.blockchainIdentifier)
            val accountId = blockchain.creator.data.extractAccounts(response).getOrThrow().firstOrNull() /* TODO: other selection criteria? */
            val account = accountId?.let { Account(it, origin.id) }

            account?.let { store.intent(OnNewActiveAccount(it)) }
        }

    private fun Peer.toConnectionId(): Connection.Id =
        when (this) {
            is P2pPeer -> Connection.Id.P2P(publicKey)
        }
}