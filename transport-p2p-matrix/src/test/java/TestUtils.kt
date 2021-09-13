
import androidx.annotation.IntRange
import it.airgap.beaconsdk.core.data.beacon.P2pPeer
import it.airgap.beaconsdk.core.internal.utils.toHexString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

// -- extensions --

internal fun <T> Flow<T>.onNth(n: Int, action: suspend (T) -> Unit): Flow<T> {
    var counter = 0
    return onEach { if (++counter == n) action(it) }
}

// -- factories --

internal fun p2pPeers(
    @IntRange(from = 1) number: Int = 1,
    version: String = "version",
    paired: Boolean = false,
): List<P2pPeer> =
    (0 until number).map {
        P2pPeer("id#$it", "name#$it", it.toHexString().asString(), "relayServer#$it", version, isPaired = paired)
    }

// -- values --

internal fun nodeApiUrl(node: String): String = "https://$node/_matrix/client/r0"