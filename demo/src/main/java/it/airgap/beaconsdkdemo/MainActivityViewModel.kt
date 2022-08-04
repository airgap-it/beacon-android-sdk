package it.airgap.beaconsdkdemo

import androidx.lifecycle.*
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
import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdk.core.message.ErrorBeaconResponse
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivityViewModel : ViewModel() {
    private val _state: MutableLiveData<MainActivity.State> = MutableLiveData(MainActivity.State())
    val state: LiveData<MainActivity.State>
        get() = _state

    private var beaconClient: BeaconWalletClient? = null
    private var awaitingRequest: BeaconRequest? = null

    fun startBeacon(): LiveData<Result<BeaconRequest>> = liveData {
        beaconClient = BeaconWalletClient("Beacon SDK Demo") {
            support(tezos(), substrate())
            use(p2pMatrix())

            ignoreUnsupportedBlockchains = true
        }

        checkForPeers()

        beaconClient?.connect()
            ?.onEach { result -> result.getOrNull()?.let { saveAwaitingRequest(it) } }
            ?.collect { emit(it) }
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
            beaconClient?.respond(response)
            removeAwaitingRequest()
        }
    }

    fun addPeer(id: String, name: String, publicKey: String, relayServer: String, version: String) {
        val peer = P2pPeer(id = id, name = name, publicKey = publicKey, relayServer = relayServer, version = version)
        viewModelScope.launch {
            beaconClient?.addPeers(peer)
            checkForPeers()
        }
    }

    fun removePeers() {
        viewModelScope.launch {
            beaconClient?.removeAllPeers()
            checkForPeers()
        }
    }

    private suspend fun checkForPeers() {
        val peers = beaconClient?.getPeers()

        val state = _state.value ?: MainActivity.State()
        _state.postValue(state.copy(hasPeers = peers?.isNotEmpty() ?: false))
    }

    private fun checkForAwaitingRequest() {
        val state = _state.value ?: MainActivity.State()
        _state.postValue(state.copy(hasAwaitingRequest = awaitingRequest != null))
    }

    private fun saveAwaitingRequest(message: BeaconMessage) {
        awaitingRequest = if (message is BeaconRequest) message else null
        checkForAwaitingRequest()
    }

    private fun removeAwaitingRequest() {
        awaitingRequest = null
        checkForAwaitingRequest()
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