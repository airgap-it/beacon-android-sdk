package it.airgap.beaconsdkdemo.dapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdkdemo.R
import it.airgap.beaconsdkdemo.utils.toJson
import kotlinx.android.synthetic.main.fragment_dapp.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DAppFragment : Fragment(R.layout.fragment_dapp) {

    private val viewModel by viewModels<DAppFragmentViewModel>()
    private val json: Json by lazy {
        Json { prettyPrint = true }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }

        resetButton.setOnClickListener { viewModel.reset() }
        pairButton.setOnClickListener { viewModel.pair() }
        requestPermissionButton.setOnClickListener { viewModel.requestPermission() }
        clearResponseButton.setOnClickListener { viewModel.clearResponse() }
    }

    private fun render(state: State) {
        with (state) {
            onBeaconResponse(beaconResponse)
            error?.let { onError(it) }

            pairButton.isEnabled = activeAccount == null
            resetButton.isEnabled = activeAccount != null
            requestPermissionButton.isEnabled = pairingRequest != null || activeAccount != null

            pairingRequestTextView.text = pairingRequest
        }
    }

    private fun onBeaconResponse(beaconResponse: BeaconResponse?) {
        clearResponseButton.isEnabled = beaconResponse != null
        messageTextView.text = beaconResponse?.let { json.encodeToString(it.toJson(json)) }
    }

    private fun onError(exception: Throwable) {
        exception.printStackTrace()
        Toast.makeText(
            requireContext(),
            "Error: ${exception.message ?: "unknown"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    data class State(
        val activeAccount: String? = null,
        val pairingRequest: String? = null,
        val beaconResponse: BeaconResponse? = null,
        val error: Throwable? = null,
    )
}
