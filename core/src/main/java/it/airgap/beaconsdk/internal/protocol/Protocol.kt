package it.airgap.beaconsdk.internal.protocol

import it.airgap.beaconsdk.internal.utils.InternalResult

internal interface Protocol {
    fun getAddressFromPublicKey(publicKey: String): InternalResult<String>

    enum class Type {
        Tezos
    }
}