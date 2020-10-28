package it.airgap.beaconsdkdemo

import androidx.lifecycle.*
import it.airgap.beaconsdk.client.BeaconClient
import it.airgap.beaconsdk.data.p2p.P2pPeerInfo
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    private val _state: MutableLiveData<MainActivity.State> = MutableLiveData(MainActivity.State())
    val state: LiveData<MainActivity.State>
        get() = _state

    private var beaconClient: BeaconClient? = null
    private var awaitingRequest: BeaconMessage.Request? = null

    fun startBeacon(): LiveData<Result<BeaconMessage>> = liveData {
        beaconClient = BeaconClient("Beacon SDK Demo")
        checkForPeers()

        beaconClient?.connect()
            ?.onEach { result -> result.getOrNull()?.let { saveAwaitingRequest(it) } }
            ?.collect { emit(it) }
    }

    fun respondExample() {
        val request = awaitingRequest ?: return

        viewModelScope.launch {
            val response = when (request) {
                is BeaconMessage.Request.Permission ->
                    BeaconMessage.Response.Permission(
                        request.id,
                        exampleTezosPublicKey,
                        request.network,
                        request.scopes,
                    )
                is BeaconMessage.Request.Operation -> TODO()
                is BeaconMessage.Request.SignPayload -> TODO()
                is BeaconMessage.Request.Broadcast -> TODO()
            }
            beaconClient?.respond(response)
            removeAwaitingRequest()
        }
    }

    fun addPeer(name: String, publicKey: String, relayServer: String) {
        val peer = P2pPeerInfo(name, publicKey, relayServer)
        viewModelScope.launch {
            beaconClient?.addPeers(peer)
            checkForPeers()
        }
    }

    fun removePeers() {
        viewModelScope.launch {
            beaconClient?.removePeers()
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
        awaitingRequest = if (message is BeaconMessage.Request) message else null
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