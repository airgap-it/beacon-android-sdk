package it.airgap.blockchain.substrate.internal.wallet

import it.airgap.beaconsdk.core.blockchain.Blockchain

internal class SubstrateWallet : Blockchain.Wallet {
    override fun addressFromPublicKey(publicKey: String): Result<String> {
        TODO("Not yet implemented")
    }
}