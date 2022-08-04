package it.airgap.beaconsdk.core.network.exception

public sealed class HttpException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {
    public data class Serialized(public val body: String) : HttpException()
    public data class Failure(public val status: Int, override val message: String? = null, override val cause: Throwable? = null) : HttpException(message, cause)
}