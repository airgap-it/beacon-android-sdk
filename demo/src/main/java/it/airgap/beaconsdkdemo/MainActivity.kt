package it.airgap.beaconsdkdemo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import it.airgap.beaconsdk.core.message.BeaconRequest
import it.airgap.beaconsdkdemo.utils.toJson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainActivityViewModel>()
    private val json: Json by lazy {
        Json { prettyPrint = true }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.state.observe(this) { render(it) }

        viewModel.startBeacon().observe(this) { result ->
            result.getOrNull()?.let { onBeaconRequest(it) }
            result.exceptionOrNull()?.let { onError(it) }
        }

        peerIdTextInput.text = examplePeerId
        peerNameTextInput.text = examplePeerName
        peerPublicKeyTextInput.text = examplePeerPublicKey
        peerRelayServerTextInput.text = examplePeerRelayServer
        peerVersionTextInput.text = examplePeerVersion

        respondButton.setOnClickListener { viewModel.respondExample() }
        addPeerButton.setOnClickListener {
            val (id, name, publicKey, relayServer, version) = getPeerInput() ?: return@setOnClickListener
            viewModel.addPeer(id, name, publicKey, relayServer, version)
        }
        removePeerButton.setOnClickListener { viewModel.removePeers() }
    }

    private fun render(state: State) {
        with (state) {
            respondButton.isEnabled = hasAwaitingRequest

            addPeerButton.isEnabled = !hasPeers
            removePeerButton.isEnabled = hasPeers

            if (!hasAwaitingRequest) messageTextView.text = null
        }
    }

    private fun onBeaconRequest(beaconRequest: BeaconRequest) {
        messageTextView.text = json.encodeToString(beaconRequest.toJson(json))
    }

    private fun onError(exception: Throwable) {
        exception.printStackTrace()
        Toast.makeText(
            this,
            "MockError: ${exception.message ?: "unknown"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getPeerInput(): PeerInput? {
        val id = peerIdTextInput.text ?: return null
        val name = peerNameTextInput.text ?: return null
        val publicKey = peerPublicKeyTextInput.text ?: return null
        val relayServer = peerRelayServerTextInput.text ?: return null
        val version = peerVersionTextInput.text ?: return null

        return PeerInput(id, name, publicKey, relayServer, version)
    }

    private var TextInputLayout.text: String?
        get() = editText?.text?.toString()
        set(value) { editText?.setText(value) }

    companion object {
        const val examplePeerId = "4adc8994-897e-5595-cec2-8bb540468473"
        const val examplePeerName = "Beacon Example Dapp"
        const val examplePeerPublicKey = "3f9e5b13a50fe06f8be0cd2061a51a5467ade7ef5fff1451949305271a634b39"
        const val examplePeerRelayServer = "beacon-node-1.sky.papers.tech"
        const val examplePeerVersion = "3"
    }

    data class State(
        val hasPeers: Boolean = false,
        val hasAwaitingRequest: Boolean = false,
    )

    data class PeerInput(val id: String, val name: String, val publicKey: String, val relayServer: String, val version: String)
}
