package it.airgap.beaconsdk.chain.tezos.data

// TODO: opt for a new `Permission` structure, add migration and move Tezos networks from `core` here.

/**
 * Type of Tezos networks supported in Beacon.
 */
//@Serializable
//public sealed class TezosNetwork : Network() {
//
//    @Serializable
//    @SerialName(Mainnet.IDENTIFIER)
//    public data class Mainnet(
//        override val name: String? = null,
//        override val rpcUrl: String? = null,
//    ) : TezosNetwork() {
//        @Transient
//        override val identifier: String = IDENTIFIER
//
//        public companion object {
//            internal const val IDENTIFIER = "mainnet"
//        }
//    }
//
//    @Serializable
//    @SerialName(Delphinet.IDENTIFIER)
//    public data class Delphinet(
//        override val name: String? = null,
//        override val rpcUrl: String? = null,
//    ) : TezosNetwork() {
//        @Transient
//        override val identifier: String = IDENTIFIER
//
//        public companion object {
//            internal const val IDENTIFIER = "delphinet"
//        }
//    }
//
//    @Serializable
//    @SerialName(Edonet.IDENTIFIER)
//    public data class Edonet(
//        override val name: String? = null,
//        override val rpcUrl: String? = null,
//    ) : TezosNetwork() {
//        @Transient
//        override val identifier: String = IDENTIFIER
//
//        public companion object {
//            internal const val IDENTIFIER = "edonet"
//        }
//    }
//
//    @Serializable
//    @SerialName(Florencenet.IDENTIFIER)
//    public data class Florencenet(
//        override val name: String? = null,
//        override val rpcUrl: String? = null,
//    ) : TezosNetwork() {
//        @Transient
//        override val identifier: String = IDENTIFIER
//
//        public companion object {
//            internal const val IDENTIFIER = "florencenet"
//        }
//    }
//
//    @Serializable
//    @SerialName(Granadanet.IDENTIFIER)
//    public data class Granadanet(
//        override val name: String? = null,
//        override val rpcUrl: String? = null,
//    ) : TezosNetwork() {
//        @Transient
//        override val identifier: String = IDENTIFIER
//
//        public companion object {
//            internal const val IDENTIFIER = "granadanet"
//        }
//    }
//
//    @Serializable
//    @SerialName(Custom.IDENTIFIER)
//    public data class Custom(
//        override val name: String? = null,
//        override val rpcUrl: String? = null,
//    ) : TezosNetwork() {
//        @Transient
//        override val identifier: String = IDENTIFIER
//
//        public companion object {
//            internal const val IDENTIFIER = "custom"
//        }
//    }
//
//    public companion object {}
//}
