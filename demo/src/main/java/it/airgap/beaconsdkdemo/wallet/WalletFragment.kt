package it.airgap.beaconsdkdemo.wallet

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdkdemo.R
import it.airgap.beaconsdkdemo.utils.toJson
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WalletFragment : Fragment(R.layout.fragment_wallet) {

    private val viewModel by viewModels<WalletFragmentViewModel>()
    private val json: Json by lazy {
        Json { prettyPrint = true }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { render(it) }

        viewModel.startBeacon().observe(viewLifecycleOwner) { result ->
            result.getOrNull()?.let { onBeaconRequest(it) }
            result.exceptionOrNull()?.let { onError(it) }
        }

        pairingRequestTextInput.text = examplePairingRequest

        respondButton.setOnClickListener { viewModel.respondExample() }
        pairButton.setOnClickListener {
            val pairingRequest = getPairingRequest() ?: return@setOnClickListener
            pairingRequestTextInput.text = pairingRequest
            viewModel.pair(pairingRequest)
        }
        unpairButton.setOnClickListener { viewModel.removePeers() }
    }

    private fun render(state: State) {
        with (state) {
            respondButton.isEnabled = hasAwaitingRequest

            pairButton.isEnabled = true
            unpairButton.isEnabled = hasPeers

            if (!hasAwaitingRequest) messageTextView.text = null
        }
    }

    private fun onBeaconRequest(beaconRequest: BeaconRequest) {
        messageTextView.text = json.encodeToString(beaconRequest.toJson(json))
    }

    private fun onError(exception: Throwable) {
        exception.printStackTrace()
        Toast.makeText(
            requireContext(),
            "Error: ${exception.message ?: "unknown"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getPairingRequest(): String? = pairingRequestTextInput.text

    companion object {
        const val examplePairingRequest = "3NDKTWt2x3EYdgULnnnHhVGJc2FYnaqKKTx99ntsB3CRoc3VZztm5FynoVj5qgciUf1KtAJMEDHicbmnKuwLnZV3YVLmThdUX3C29zadfhEwRkng4oqUSzfRMFiUzL9QrpjMxXzdn4MZiCHoxm9r2mrZ56vnzwGWPo7vKUFhWPXUWj2JfMaih4hRnRt5H1Y3DiXmuYGwfEEAXMjXDNvsuBjkv6NtQ6t3zUnrqhJd26RCyhvdkfPaB1HjxyEsUQQeu9HdKNUR9CfXrkNTDqFzcCWUTF1Uo9whBFEpx9dqbZb7dG1S2i8kqVMxoheYCZxjUNzT5dD"
    }

    private var TextInputLayout.text: String?
        get() = editText?.text?.toString()
        set(value) { editText?.setText(value) }

    data class State(
        val hasPeers: Boolean = false,
        val hasAwaitingRequest: Boolean = false,
    )
}
