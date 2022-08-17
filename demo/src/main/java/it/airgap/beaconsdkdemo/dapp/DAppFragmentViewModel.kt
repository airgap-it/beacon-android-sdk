package it.airgap.beaconsdkdemo.dapp

import androidx.lifecycle.*
import it.airgap.beaconsdk.blockchain.substrate.substrate
import it.airgap.beaconsdk.blockchain.tezos.extension.requestTezosPermission
import it.airgap.beaconsdk.blockchain.tezos.tezos
import it.airgap.beaconsdk.client.dapp.BeaconDAppClient
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix
import it.airgap.beaconsdkdemo.utils.setValue
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DAppFragmentViewModel : ViewModel() {
    private val _state: MutableLiveData<DAppFragment.State> = MutableLiveData(DAppFragment.State())
    val state: LiveData<DAppFragment.State>
        get() = _state

    private var beaconClient: BeaconDAppClient? = null
    private var awaitingResponse: BeaconResponse? = null

    fun startBeacon(): LiveData<Result<BeaconResponse>> = liveData {
        beaconClient = BeaconDAppClient("Beacon SDK Demo (DApp)", clientId = "__dapp__") {
            support(tezos(), substrate())
            use(p2pMatrix())

            ignoreUnsupportedBlockchains = true
        }

        checkForActiveAccount()

        beaconClient?.connect()
            ?.onEach { result -> result.getOrNull()?.let {
                saveAwaitingResponse(it)
                checkForActiveAccount()
            } }
            ?.collect { emit(it) }
    }

    fun pair() {
        viewModelScope.launch {
            val beaconClient = beaconClient ?: return@launch

            val pairingRequest = beaconClient.pair()
            val serializerPairingRequest = beaconClient.serializePairingData(pairingRequest)

            _state.setValue { copy(pairingRequest = serializerPairingRequest) }
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
            checkForActiveAccount()
        }
    }

    private suspend fun checkForActiveAccount() {
        val activeAccount = beaconClient?.getActiveAccount()
        _state.setValue { copy(activeAccount = activeAccount) }
    }

    private fun checkForAwaitingResponses() {
        _state.setValue { copy(hasAwaitingResponses = awaitingResponse != null) }
    }

    private fun saveAwaitingResponse(message: BeaconMessage) {
        awaitingResponse = if (message is BeaconResponse) message else null
        checkForAwaitingResponses()
    }

    private fun MutableLiveData<DAppFragment.State>.setValue(update: DAppFragment.State.() -> DAppFragment.State) {
        setValue(DAppFragment.State(), update)
    }
}