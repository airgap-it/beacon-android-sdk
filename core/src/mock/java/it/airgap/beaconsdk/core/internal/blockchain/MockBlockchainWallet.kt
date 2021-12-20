package it.airgap.beaconsdk.core.internal.blockchain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockBlockchainWallet : Blockchain.Wallet {
    override fun addressFromPublicKey(publicKey: String): Result<String> =
        Result.success("@$publicKey")
}