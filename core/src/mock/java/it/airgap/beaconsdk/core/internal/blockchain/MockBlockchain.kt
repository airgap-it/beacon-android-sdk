package it.airgap.beaconsdk.core.internal.blockchain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.blockchain.Blockchain
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.utils.IdentifierCreator

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MockBlockchain : Blockchain {
    override val identifier: String = IDENTIFIER
    override val wallet: Blockchain.Wallet = MockBlockchainWallet()
    override val creator: Blockchain.Creator = MockBlockchainCreator()
    override val serializer: Blockchain.Serializer = MockBlockchainSerializer()

    public class Factory : Blockchain.Factory<MockBlockchain> {
        override val identifier: String = IDENTIFIER
        override fun create(dependencyRegistry: DependencyRegistry): MockBlockchain = MockBlockchain()
    }

    public companion object {
        public const val IDENTIFIER: String = "mockBlockchain"
    }
}
