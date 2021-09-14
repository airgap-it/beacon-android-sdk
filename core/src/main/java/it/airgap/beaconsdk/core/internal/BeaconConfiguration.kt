package it.airgap.beaconsdk.core.internal

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY)
public object BeaconConfiguration {

    // -- SDK --

    public const val STORAGE_NAME: String = "beaconsdk"

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val sdkVersion: String
        get() = BuildConfig.VERSION_NAME

    internal val cryptoProvider: CryptoProvider = CryptoProvider.LazySodium
    internal val serializerProvider: SerializerProvider = SerializerProvider.Base58Check
    internal val httpClientProvider: HttpClientProvider = HttpClientProvider.Ktor

    internal enum class CryptoProvider {
        LazySodium
    }

    internal enum class SerializerProvider {
        Base58Check
    }

    internal enum class HttpClientProvider {
        Ktor
    }
}