package it.airgap.beaconsdk.internal.protocol

import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.utils.Base58Check

internal class ProtocolRegistry(private val crypto: Crypto, private val base58Check: Base58Check) {
    private val protocolMap: MutableMap<Protocol.Type, Protocol> = mutableMapOf()

    fun get(type: Protocol.Type): Protocol = protocolMap.getOrPut(type) {
        when (type) {
            Protocol.Type.Tezos -> TezosProtocol(crypto, base58Check)
        }
    }
}