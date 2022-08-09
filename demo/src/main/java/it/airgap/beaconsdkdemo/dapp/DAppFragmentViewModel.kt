package it.airgap.beaconsdkdemo.dapp

import androidx.lifecycle.*
import it.airgap.beaconsdk.blockchain.substrate.substrate
import it.airgap.beaconsdk.blockchain.tezos.client.requestTezosPermission
import it.airgap.beaconsdk.blockchain.tezos.tezos
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix
import it.airgap.client.dapp.BeaconDAppClient
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DAppFragmentViewModel : ViewModel() {
    private val _state: MutableLiveData<DAppFragment.State> = MutableLiveData(DAppFragment.State())
    val state: LiveData<DAppFragment.State>
        get() = _state

    private var beaconClient: BeaconDAppClient? = null
    private var awaitingResponse: BeaconResponse? = null

    fun startBeacon(): LiveData<Result<BeaconResponse>> = liveData {
        beaconClient = BeaconDAppClient("Beacon SDK Demo (DApp)") {
            support(tezos(), substrate())
            use(p2pMatrix())

            ignoreUnsupportedBlockchains = true
        }

        checkForActiveAccount()

        beaconClient?.connect()
            ?.onEach { result -> result.getOrNull()?.let { saveAwaitingResponse(it) } }
            ?.collect { emit(it) }
    }

    fun pair() {
        viewModelScope.launch {
            val beaconClient = beaconClient ?: return@launch

            val pairingRequest = beaconClient.pair()
            val serializerPairingRequest = beaconClient.serializePairingData(pairingRequest)

            val state = _state.value ?: DAppFragment.State()
            _state.postValue(state.copy(pairingRequest = serializerPairingRequest))
        }
    }

    fun requestPermission() {
        viewModelScope.launch {
            beaconClient?.requestTezosPermission()
        }
    }

    fun clearResponse() {
        awaitingResponse = null
        checkForAwaitingResponses()
    }

    fun reset() {
        viewModelScope.launch {
            beaconClient?.reset()
        }
    }

    private suspend fun checkForActiveAccount() {
        val activeAccount = beaconClient?.getActiveAccount()

        val state = _state.value ?: DAppFragment.State()
        _state.postValue(state.copy(hasActiveAccount = activeAccount != null))
    }

    private fun checkForAwaitingResponses() {
        val state = _state.value ?: DAppFragment.State()
        _state.postValue(state.copy(hasAwaitingResponses = awaitingResponse != null))
    }

    private fun saveAwaitingResponse(message: BeaconMessage) {
        awaitingResponse = if (message is BeaconResponse) message else null
        checkForAwaitingResponses()
    }
}