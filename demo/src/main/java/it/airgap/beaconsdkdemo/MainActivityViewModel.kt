package it.airgap.beaconsdkdemo

import androidx.lifecycle.*
import it.airgap.beaconsdk.core.client.BeaconClient
import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.message.*
import it.airgap.beaconsdk.core.message.response.permission.PermissionBeaconResponse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    private val _state: MutableLiveData<MainActivity.State> = MutableLiveData(MainActivity.State())
    val state: LiveData<MainActivity.State>
        get() = _state

    private var beaconClient: BeaconClient? = null
    private var awaitingRequest: BeaconRequest? = null

    fun startBeacon(): LiveData<Result<BeaconRequest>> = liveData {
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
                is PermissionBeaconRequest -> PermissionBeaconResponse.from(request, exampleTezosPublicKey)
                is OperationBeaconRequest -> TODO()
                is SignPayloadBeaconRequest -> TODO()
                is BroadcastBeaconRequest -> TODO()
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