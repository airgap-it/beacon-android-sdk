package it.airgap.beaconsdk.blockchain.substrate.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator(SubstrateSignerPayload.CLASS_DISCRIMINATOR)
public sealed class SubstrateSignerPayload {

    @Serializable
    @SerialName(Json.TYPE)
    public data class Json(
        public val blockHash: String,
        public val blockNumber: String,
        public val era: String,
        public val genesisHash: String,
        public val method: String,
        public val nonce: String,
        public val specVersion: String,
        public val tip: String,
        public val transactionVersion: String,
        public val signedExtensions: List<String>,
        public val version: Long,
    ) : SubstrateSignerPayload() {
        public companion object {
            internal const val TYPE = "json"
        }
    }

    @Serializable
    @SerialName(Raw.TYPE)
    public data class Raw(
        public val isMutable: Boolean,
        public val dataType: DataType,
        public val data: String,
    ) : SubstrateSignerPayload() {

        @Serializable
        public enum class DataType {
            @SerialName("bytes") Bytes,
            @SerialName("payload") Payload;

            public companion object {}
        }

        public companion object {
            internal const val TYPE = "raw"
        }
    }

    public companion object {
        internal const val CLASS_DISCRIMINATOR = "type"
    }
}
