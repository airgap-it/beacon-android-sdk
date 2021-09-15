package it.airgap.beaconsdk.core.internal.chain

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.data.BeaconError
import it.airgap.beaconsdk.core.internal.di.DependencyRegistry
import it.airgap.beaconsdk.core.internal.message.VersionedBeaconMessage
import it.airgap.beaconsdk.core.internal.message.v1.V1BeaconMessage
import it.airgap.beaconsdk.core.internal.message.v2.V2BeaconMessage
import it.airgap.beaconsdk.core.message.BeaconMessage
import it.airgap.beaconsdk.core.message.ChainBeaconRequest
import it.airgap.beaconsdk.core.message.ChainBeaconResponse
import kotlinx.serialization.KSerializer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Chain<W : Chain.Wallet, S : Chain.Serializer> {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val identifier: String

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val wallet: W

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val serializer: S

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Wallet {
        public fun addressFromPublicKey(publicKey: String): Result<String>
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Serializer {

        // -- BeaconMessage --

        public val requestPayload: KSerializer<ChainBeaconRequest.Payload>
        public val responsePayload: KSerializer<ChainBeaconResponse.Payload>

        // -- data --

        public val error: KSerializer<BeaconError>

        // -- VersionedBeaconMessage --

        public val v1: VersionedBeaconMessage.Factory<BeaconMessage, V1BeaconMessage>
        public val v2: VersionedBeaconMessage.Factory<BeaconMessage, V2BeaconMessage>
    }

    public interface Factory<T: Chain<*, *>> {
        public val identifier: String

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(dependencyRegistry: DependencyRegistry): T
    }
}