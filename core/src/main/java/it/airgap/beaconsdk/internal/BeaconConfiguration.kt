package it.airgap.beaconsdk.internal

import it.airgap.beaconsdk.BuildConfig

internal object BeaconConfiguration {
    // -- SDK --

    const val STORAGE_NAME = "beaconsdk"

    val sdkVersion: String
        get() = BuildConfig.VERSION_NAME

    // -- P2P --
    const val MATRIX_CLIENT_API: String = "/_matrix/client/r0"
    const val MATRIX_MAX_SYNC_RETRIES: Int = 3

    const val P2P_JOIN_DELAY_MS: Long = 200
    const val P2P_MAX_JOIN_RETRIES: Int = 10

    const val P2P_REPLICATION_COUNT: Int = 1
    val defaultRelayServers: List<String> = listOf(
        "beacon-node-1.sky.papers.tech",
//        "beacon-node-0.papers.tech:8448",
    )

    // -- common --

    val cryptoProvider: CryptoProvider = CryptoProvider.LazySodium
    val serializerProvider: SerializerProvider = SerializerProvider.Base58Check
    val httpClientProvider: HttpClientProvider = HttpClientProvider.Ktor

    enum class CryptoProvider {
        LazySodium
    }

    enum class SerializerProvider {
        Base58Check
    }

    enum class HttpClientProvider {
        Ktor
    }
}