package it.airgap.beaconsdkdemo

import androidx.lifecycle.*
import it.airgap.beaconsdk.chain.tezos.tezos
import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.data.P2P
import it.airgap.beaconsdk.core.data.P2pPeer
import it.airgap.beaconsdk.core.message.*
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    private val _state: MutableLiveData<MainActivity.State> = MutableLiveData(MainActivity.State())
    val state: LiveData<MainActivity.State>
        get() = _state

    private var beaconClient: BeaconWalletClient? = null
    private var awaitingRequest: BeaconRequest? = null

    fun startBeacon(): LiveData<Result<BeaconRequest>> = liveData {
        beaconClient = BeaconWalletClient(
            "Beacon SDK Demo",
            listOf(
                tezos(),
            ),
        ) {
            addConnections(
                P2P(p2pMatrix()),
            )
        }

        checkForPeers()

        beaconClient?.connect()
            ?.onEach { result -> result.getOrNull()?.let { saveAwaitingRequest(it) } }
            ?.collect { emit(it) }
    }

    fun respondExample() {
        val request = awaitingRequest ?: return

        viewModelScope.launch {
            val response = when (request) {
                is PermissionBeaconRequest -> PermissionBeaconResponse.from(request, exampleTezosPublicKey)
                is ChainBeaconRequest -> TODO()
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
        const val exampleTezosPublicKey = "edpktpzo8UZieYaJZgCHP6M6hKHPdWBSNqxvmEt6dwWRgxDh1EAFw9"
    }
}