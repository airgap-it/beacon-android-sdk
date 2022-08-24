package it.airgap.beaconsdkdemo.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textfield.TextInputLayout
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdkdemo.R
import it.airgap.beaconsdkdemo.utils.toJson
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WalletFragment : Fragment(R.layout.fragment_wallet) {

    private val viewModel by viewModels<WalletFragmentViewModel>()
    private val json: Json by lazy {
        Json { prettyPrint = true }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            onBeaconRequest(beaconRequest)

            pairButton.isEnabled = true
            unpairButton.isEnabled = hasPeers

            error?.let { onError(it) }
        }
    }

    private fun onBeaconRequest(beaconRequest: BeaconRequest?) {
        respondButton.isEnabled = beaconRequest != null
        messageTextView.text = beaconRequest?.let { json.encodeToString(it.toJson(json)) }
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
        const val examplePairingRequest = "3NDKTWt2x3LCHC2AMJk1U6off23vDCn6FEnR8TE7p4xyRS8Na5MJwcdh2ovvofd5ieieSKgZy2Y6uyoEDtT2kUHdNgB4Uvurbp3qdGkurh4YD9JdaSjk8tyWMhbbnLmJnKoWSiJpPGnNRYCt7QNx6Rg8hNdPCp5yHEfB4dKrr7nyB48gJNTEJ5wVCzfxAxyeQGjn41LC5d47qZD7iK5YNRg3QLRNPsNjEtGWUPPtagWB13Aua8csinhhmVgedhBwYrvDd5TmVnGdYBHKfTeugtYrF4bkgeNvbM5RL4TmBpNiFZ86qnRHo22dwESfmHfeW91gfpD"
    }

    private var TextInputLayout.text: String?
        get() = editText?.text?.toString()
        set(value) { editText?.setText(value) }

    data class State(
        val hasPeers: Boolean = false,
        val beaconRequest: BeaconRequest? = null,
        val error: Throwable? = null,
    )
}
