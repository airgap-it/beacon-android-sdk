package it.airgap.beaconsdk.internal.transport.client

import it.airgap.beaconsdk.internal.crypto.Crypto
import it.airgap.beaconsdk.internal.crypto.data.KeyPair

internal abstract class CommunicationClient(protected val crypto: Crypto, protected val keyPair: KeyPair)