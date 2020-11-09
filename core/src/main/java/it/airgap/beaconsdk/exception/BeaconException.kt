package it.airgap.beaconsdk.exception

public class BeaconException(message: String? = null, cause: Throwable? = null)
    : Exception(message ?: if (cause != null) null else MESSAGE_UNKNOWN, cause) {

    public companion object {
        private const val MESSAGE_UNKNOWN = "Unknown error"
    }
}