package it.airgap.beaconsdkdemo.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateAccount
import it.airgap.beaconsdk.blockchain.substrate.data.SubstrateNetwork
import it.airgap.beaconsdk.blockchain.substrate.message.request.PermissionSubstrateRequest
import it.airgap.beaconsdk.blockchain.substrate.message.response.PermissionSubstrateResponse
import it.airgap.beaconsdk.blockchain.substrate.substrate
import it.airgap.beaconsdk.blockchain.tezos.data.TezosAccount
import it.airgap.beaconsdk.blockchain.tezos.data.TezosError
import it.airgap.beaconsdk.blockchain.tezos.data.TezosNetwork
import it.airgap.beaconsdk.blockchain.tezos.extension.from
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.response.PermissionTezosResponse
import it.airgap.beaconsdk.blockchain.tezos.tezos
import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.message.ErrorBeaconResponse
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix
import it.airgap.beaconsdkdemo.utils.emit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class WalletFragmentViewModel : ViewModel() {
    private val _state: MutableStateFlow<WalletFragment.State> = MutableStateFlow(WalletFragment.State())
    val state: Flow<WalletFragment.State>
        get() = _state

    private var beaconClient: BeaconWalletClient? = null
    private var awaitingRequest: BeaconRequest? = null

    init {
        viewModelScope.launch {
            val client = BeaconWalletClient("Beacon SDK Demo (Wallet)", clientId = "__wallet__") {
                support(tezos(), substrate())
                use(p2pMatrix())

                ignoreUnsupportedBlockchains = true
            }.also { beaconClient = it }

            client.connect()
                .onStart { checkForPeers() }
                .collect { result ->
                    result.getOrNull()?.let { onBeaconRequest(it) }
                    result.exceptionOrNull()?.let { onError(it) }
                }
        }
    }

    fun respondExample() {
        val request = awaitingRequest ?: return
        val beaconClient = beaconClient ?: return

        viewModelScope.launch {
            val response = when (request) {

                /* Tezos */

                is PermissionTezosRequest -> PermissionTezosResponse.from(request, exampleTezosAccount(request.network, beaconClient))
                is OperationTezosRequest -> ErrorBeaconResponse.from(request, BeaconError.Aborted)
                is SignPayloadTezosRequest -> ErrorBeaconResponse.from(request, TezosError.SignatureTypeNotSupported)
                is BroadcastTezosRequest -> ErrorBeaconResponse.from(request, TezosError.BroadcastError)

                /* Substrate*/

                is PermissionSubstrateRequest -> PermissionSubstrateResponse.from(request, listOf(exampleSubstrateAccount(request.networks.first(), beaconClient)))

                /* Others */
                else -> ErrorBeaconResponse.from(request, BeaconError.Unknown)
            }

            try {
                beaconClient.respond(response)
                removeAwaitingRequest()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }

    fun pair(pairingRequest: String) {
        viewModelScope.launch {
            try {
                beaconClient?.pair(pairingRequest)
                checkForPeers()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }

    fun removePeers() {
        viewModelScope.launch {
            beaconClient?.removeAllPeers()
            checkForPeers()
        }
    }

    private fun onBeaconRequest(request: BeaconRequest) {
        awaitingRequest = request
        _state.emit { copy(beaconRequest = request) }
    }

    private fun onError(error: Throwable) {
        _state.emit { copy(error = error) }
    }

    private suspend fun checkForPeers() {
        val peers = beaconClient?.getPeers()
        _state.emit { copy(hasPeers = peers?.isNotEmpty() ?: false) }
    }

    private fun removeAwaitingRequest() {
        awaitingRequest = null
        _state.emit { copy(beaconRequest = null) }
    }

    private fun MutableStateFlow<WalletFragment.State>.emit(update: WalletFragment.State.() -> WalletFragment.State) {
        emit(WalletFragment.State(), update)
    }

    companion object {
        fun exampleTezosAccount(network: TezosNetwork, client: BeaconClient<*>): TezosAccount = TezosAccount(
            "edpktpzo8UZieYaJZgCHP6M6hKHPdWBSNqxvmEt6dwWRgxDh1EAFw9",
            "tz1Mg6uXUhJzuCh4dH2mdBdYBuaiVZCCZsak",
            network,
            client,
        )

        fun exampleSubstrateAccount(network: SubstrateNetwork, client: BeaconClient<*>): SubstrateAccount = SubstrateAccount(
            "724867a19e4a9422ac85f3b9a7c4bf5ccf12c2df60d858b216b81329df716535",
            "13aqy7vzMjuS2Nd6TYahHHetGt7dTgaqijT6Tpw3NS2MDFug",
            network,
            client,
        )
    }
}