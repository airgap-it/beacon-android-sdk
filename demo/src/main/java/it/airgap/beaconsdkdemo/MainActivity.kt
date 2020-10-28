package it.airgap.beaconsdkdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.textfield.TextInputLayout
import it.airgap.beaconsdk.message.BeaconMessage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
            result.getOrNull()?.let { onBeaconMessage(it) }
            result.exceptionOrNull()?.let { onError(it) }
        }

        peerNameTextInput.text = examplePeerName
        peerPublicKeyTextInput.text = examplePeerPublicKey
        peerRelayServerTextInput.text = examplePeerRelayServer

        respondButton.setOnClickListener { viewModel.respondExample() }
        addPeerButton.setOnClickListener {
            val (name, publicKey, relayServer) = getPeerInput() ?: return@setOnClickListener
            viewModel.addPeer(name, publicKey, relayServer)
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


    private fun onBeaconMessage(beaconMessage: BeaconMessage) {
        messageTextView.text = json.encodeToString(beaconMessage)
    }

    private fun onError(exception: Throwable) {
        Toast.makeText(
            this,
            "Error: ${exception.message ?: "unknown"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getPeerInput(): Triple<String, String, String>? {
        val name = peerNameTextInput.text ?: return null
        val publicKey = peerPublicKeyTextInput.text ?: return null
        val relayServer = peerRelayServerTextInput.text ?: return null

        return Triple(name, publicKey, relayServer)
    }

    private var TextInputLayout.text: String?
        get() = editText?.text?.toString()
        set(value) { editText?.setText(value) }

    companion object {
        const val examplePeerName = "Beacon Example Dapp"
        const val examplePeerPublicKey = "5760d150b0f6906ba98c8220cbf197a87d54631af73593b031c0175ebd29b3b0"
        const val examplePeerRelayServer = "matrix.papers.tech"
    }

    data class State(
        val hasPeers: Boolean = false,
        val hasAwaitingRequest: Boolean = false,
    )
}
