package it.airgap.client.dapp.internal.controller.account

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.Account
import it.airgap.beaconsdk.core.data.Origin
import it.airgap.beaconsdk.core.internal.blockchain.BlockchainRegistry
import it.airgap.beaconsdk.core.message.PermissionBeaconResponse
import it.airgap.beaconsdk.core.transport.data.PairingResponse
import it.airgap.client.dapp.internal.controller.account.store.AccountControllerStore
import it.airgap.client.dapp.internal.controller.account.store.OnNewActiveAccount
import it.airgap.client.dapp.internal.controller.account.store.OnPeerPaired

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AccountController(private val store: AccountControllerStore, private val blockchainRegistry: BlockchainRegistry) {

    public suspend fun getRequestDestination(): Origin? =
        store.state().getOrThrow().defaultDestination

    public suspend fun getActiveAccountId(): String? =
        store.state().getOrThrow().activeAccount?.accountId

    public suspend fun onPairingResponse(pairingResponse: PairingResponse): Result<Unit> =
        runCatching {
            store.intent(OnPeerPaired(pairingResponse.toPeer()))
        }

    public suspend fun onPermissionResponse(origin: Origin, response: PermissionBeaconResponse): Result<Unit> =
        runCatching {
            val blockchain = blockchainRegistry.get(response.blockchainIdentifier)
            val accountId = blockchain.creator.data.extractAccounts(response).getOrThrow().firstOrNull() /* TODO: other selection criteria? */
            val account = accountId?.let { Account(it, origin.id) }

            account?.let { store.intent(OnNewActiveAccount(it)) }
        }
}