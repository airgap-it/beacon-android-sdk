package it.airgap.beaconsdk.core.internal

import it.airgap.beaconsdk.BuildConfig

public object BeaconConfiguration {
    // -- SDK --

    const val STORAGE_NAME = "beaconsdk"

    val sdkVersion: String
        get() = BuildConfig.VERSION_NAME

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