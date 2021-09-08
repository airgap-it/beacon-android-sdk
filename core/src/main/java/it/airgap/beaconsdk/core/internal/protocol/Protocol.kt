package it.airgap.beaconsdk.core.internal.protocol

internal interface Protocol {
    fun getAddressFromPublicKey(publicKey: String): Result<String>

    enum class Type {
        Tezos,
    }
}