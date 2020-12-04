package it.airgap.beaconsdk.internal

import it.airgap.beaconsdk.BuildConfig

internal object BeaconConfiguration {
    val sdkVersion: String
        get() = BuildConfig.VERSION_NAME

    val beaconVersion: String = "1"

    val matrixClientApi: String = "/_matrix/client/r0"
    val matrixMaxSyncRetries: Int = 3
    val defaultRelayServers: List<String> = listOf(
        "matrix.papers.tech",
//        "matrix.tez.ie",
//        "matrix-dev.papers.tech",
//        "matrix.stove-labs.com",
//        "yadayada.cryptonomic-infra.tech",
    )
    val p2pReplicationCount: Int = 1

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