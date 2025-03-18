package it.airgap.beaconsdkdemo.dapp

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
import it.airgap.beaconsdk.core.message.BeaconResponse
import it.airgap.beaconsdkdemo.R
import it.airgap.beaconsdkdemo.databinding.FragmentDappBinding
import it.airgap.beaconsdkdemo.utils.toJson
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DAppFragment : Fragment(R.layout.fragment_dapp) {
    private var _binding: FragmentDappBinding? = null
    private val binding: FragmentDappBinding
        get() = _binding!!

    private val viewModel by viewModels<DAppFragmentViewModel>()
    private val json: Json by lazy {
        Json { prettyPrint = true }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDappBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }

        binding.resetButton.setOnClickListener { viewModel.reset() }
        binding.pairButton.setOnClickListener { viewModel.pair() }
        binding.requestPermissionButton.setOnClickListener { viewModel.requestPermission() }
        binding.clearResponseButton.setOnClickListener { viewModel.clearResponse() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: State) {
        with (state) {
            onBeaconResponse(beaconResponse)
            error?.let { onError(it) }

            binding.pairButton.isEnabled = activeAccount == null
            binding.resetButton.isEnabled = activeAccount != null
            binding.requestPermissionButton.isEnabled = pairingRequest != null || activeAccount != null

            binding.pairingRequestTextView.text = pairingRequest
        }
    }

    private fun onBeaconResponse(beaconResponse: BeaconResponse?) {
        binding.clearResponseButton.isEnabled = beaconResponse != null
        binding.messageTextView.text = beaconResponse?.let { json.encodeToString(it.toJson(json)) }
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
