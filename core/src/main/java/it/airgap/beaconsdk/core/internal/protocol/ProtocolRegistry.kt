package it.airgap.beaconsdk.core.internal.protocol

import it.airgap.beaconsdk.core.internal.crypto.Crypto
import it.airgap.beaconsdk.core.internal.utils.Base58Check

internal class ProtocolRegistry(private val crypto: Crypto, private val base58Check: Base58Check) {
    private val protocolMap: MutableMap<Protocol.Type, Protocol> = mutableMapOf()

    fun get(type: Protocol.Type): Protocol = protocolMap.getOrPut(type) {
        when (type) {
            Protocol.Type.Tezos -> Tezos(crypto, base58Check)
        }
    }
}