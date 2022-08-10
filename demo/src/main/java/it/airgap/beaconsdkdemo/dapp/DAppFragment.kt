package it.airgap.beaconsdkdemo.dapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdkdemo.R
import it.airgap.beaconsdkdemo.utils.toJson
import kotlinx.android.synthetic.main.fragment_dapp.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DAppFragment : Fragment(R.layout.fragment_dapp) {

    private val viewModel by viewModels<DAppFragmentViewModel>()
    private val json: Json by lazy {
        Json { prettyPrint = true }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { render(it) }

        viewModel.startBeacon().observe(viewLifecycleOwner) { result ->
            result.getOrNull()?.let { onBeaconResponse(it) }
            result.exceptionOrNull()?.let { onError(it) }
        }

        resetButton.setOnClickListener { viewModel.reset() }
        pairButton.setOnClickListener { viewModel.pair() }
        requestPermissionButton.setOnClickListener { viewModel.requestPermission() }
        clearResponseButton.setOnClickListener { viewModel.clearResponse() }
    }

    private fun render(state: State) {
        with (state) {
            clearResponseButton.isEnabled = hasAwaitingResponses

            pairButton.isEnabled = activeAccount == null
            resetButton.isEnabled = activeAccount != null
            requestPermissionButton.isEnabled = pairingRequest != null || activeAccount != null

            pairingRequestTextView.text = pairingRequest

            if (!hasAwaitingResponses) messageTextView.text = null
        }
    }

    private fun onBeaconResponse(beaconResponse: BeaconResponse) {
        messageTextView.text = json.encodeToString(beaconResponse.toJson(json))
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
        val hasAwaitingResponses: Boolean = false,
    )
}
