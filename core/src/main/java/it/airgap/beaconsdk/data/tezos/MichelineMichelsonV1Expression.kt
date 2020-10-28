package it.airgap.beaconsdk.data.tezos

import it.airgap.beaconsdk.internal.utils.failWithExpectedJsonDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

@Serializable(with = MichelineMichelsonV1Expression.Serializer::class)
sealed class MichelineMichelsonV1Expression {

    @Serializable
    data class PrimitiveInt(val int: String) : MichelineMichelsonV1Expression() {
        companion object {}
    }

    @Serializable
    data class PrimitiveString(val string: String) : MichelineMichelsonV1Expression() {
        companion object {}
    }

    @Serializable
    data class PrimitiveBytes(val bytes: String) : MichelineMichelsonV1Expression() {
        companion object {}
    }

    @Serializable
    data class PrimitiveApplication(
        val prim: Primitive,
        val args: List<MichelineMichelsonV1Expression>?,
        val annots: List<String>?
    ) : MichelineMichelsonV1Expression() {
        companion object {}
    }

    @Serializable
    data class Node(val expressions: List<MichelineMichelsonV1Expression>) : MichelineMichelsonV1Expression() {
        companion object {}
    }

    companion object {}

    @Serializable
    enum class Primitive {
        @SerialName("Unit") DataUnit,
        @SerialName("True") DataTrue,
        @SerialName("False") DataFalse,
        @SerialName("Pair") DataPair,
        @SerialName("Left") DataLeft,
        @SerialName("Right") DataRight,
        @SerialName("Some") DataSome,
        @SerialName("None") DataNone,
        @SerialName("Elt") DataElt,

        @SerialName("DROP") InsDrop,
        @SerialName("DUP") InsDup,
        @SerialName("SWAP") InsSwap,
        @SerialName("DIG") InsDig,
        @SerialName("PUSH") InsPush,
        @SerialName("SOME") InsSome,
        @SerialName("NONE") InsNone,
        @SerialName("UNIT") InsUnit,
        @SerialName("IF_NONE") InsIfNone,
        @SerialName("PAIR") InsPair,
        @SerialName("CAR") InsCar,
        @SerialName("CDR") InsCdr,
        @SerialName("LEFT") InsLeft,
        @SerialName("RIGHT") InsRight,
        @SerialName("IF_LEFT") InsIfLeft,
        @SerialName("NIL") InsNil,
        @SerialName("CONS") InsCons,
        @SerialName("IF_CONS") InsIfCons,
        @SerialName("SIZE") InsSize,
        @SerialName("EMPTY_SET") InsEmptySet,
        @SerialName("EMPTY_MAP") InsEmptyMap,
        @SerialName("EMPTY_BIG_MAP") InsEmptyBigMap,
        @SerialName("MAP") InsMap,
        @SerialName("ITER") InsIter,
        @SerialName("MEM") InsMem,
        @SerialName("GET") InsGet,
        @SerialName("UPDATE") InsUpdate,
        @SerialName("IF") InsIf,
        @SerialName("LOOP") InsLoop,
        @SerialName("LOOP_LEFT") InsLoopLeft,
        @SerialName("LAMBDA") InsLambda,
        @SerialName("EXEC") InsExec,
        @SerialName("DIP") InsDip,
        @SerialName("FAIL_WITH") InsFailWith,
        @SerialName("CAST") InsCast,
        @SerialName("RENAME") InsRename,
        @SerialName("CONCAT") InsConcat,
        @SerialName("SLICE") InsSlice,
        @SerialName("PACK") InsPack,
        @SerialName("UNPACK") InsUnpack,
        @SerialName("ADD") InsAdd,
        @SerialName("SUB") InsSub,
        @SerialName("MUL") InsMul,
        @SerialName("EDIV") InsEdiv,
        @SerialName("ABS") InsAbs,
        @SerialName("IS_NAT") InsIsNat,
        @SerialName("INT") InsInt,
        @SerialName("NEG") InsNeg,
        @SerialName("LSL") InsLsl,
        @SerialName("LSR") InsLsr,
        @SerialName("OR") InsOr,
        @SerialName("AND") InsAnd,
        @SerialName("XOR") InsXor,
        @SerialName("NOT") InsNot,
        @SerialName("COMPARE") InsCompare,
        @SerialName("EW") InsEq,
        @SerialName("NEQ") InsNeq,
        @SerialName("LT") InsLt,
        @SerialName("GT") InsGt,
        @SerialName("LE") InsLe,
        @SerialName("GE") InsGe,
        @SerialName("SELF") InsSelf,
        @SerialName("CONTRACT") InsContract,
        @SerialName("TRANSFER_TOKENS") InsTransferTokens,
        @SerialName("SET_DELEGATE") InsSetDelegate,
        @SerialName("CREATE_ACCOUNT") InsCreateAccount,
        @SerialName("CREATE_CONTRACT") InsCreateContract,
        @SerialName("IMPLICIT_ACCOUNT") InsImplicitAccount,
        @SerialName("NOW") InsNow,
        @SerialName("AMOUNT") InsAmount,
        @SerialName("BALANCE") InsBalance,
        @SerialName("CHECK_SIGNATURE") InsCheckSignature,
        @SerialName("BLAKE2B") InsBlake2b,
        @SerialName("SHA256") InsSha256,
        @SerialName("SHA512") InsSha512,
        @SerialName("HASH_KEY") InsHashKey,
        @SerialName("STEPS_TO_QUOTA") InsStepsToQuota,
        @SerialName("SOURCE") InsSource,
        @SerialName("SENDER") InsSender,
        @SerialName("ADDRESS") InsAddress,
        @SerialName("CHAIN_ID") InsChainId,

        @SerialName("int") TypeInt,
        @SerialName("nat") TypeNat,
        @SerialName("string") TypeString,
        @SerialName("bytes") TypeBytes,
        @SerialName("mutez") TypeMutez,
        @SerialName("bool") TypeBool,
        @SerialName("key_hash") TypeKeyHash,
        @SerialName("timestamp") TypeTimestamp,
        @SerialName("address") TypeAddress,
        @SerialName("key") TypeKey,
        @SerialName("unit") TypeUnit,
        @SerialName("signature") TypeSignature,
        @SerialName("option") TypeOption,
        @SerialName("list") TypeList,
        @SerialName("set") TypeSet,
        @SerialName("operation") TypeOperation,
        @SerialName("contract") TypeContract,
        @SerialName("pair") TypePair,
        @SerialName("or") TypeOr,
        @SerialName("lambda") TypeLambda,
        @SerialName("map") TypeMap,
        @SerialName("big_map") TypeBigMap,
        @SerialName("chain_id") TypeChainId
    }

    class Serializer : KSerializer<MichelineMichelsonV1Expression> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MichelineMichelsonV1Expression", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): MichelineMichelsonV1Expression {
            val jsonDecoder = decoder as? JsonDecoder ?: failWithExpectedJsonDecoder(decoder::class)
            val jsonElement = jsonDecoder.decodeJsonElement()

            val jsonObject = jsonElement.jsonObject

            return when {
                jsonObject.containsKey("int") -> jsonDecoder.json.decodeFromJsonElement(PrimitiveInt.serializer(), jsonElement)
                jsonObject.containsKey("string") -> jsonDecoder.json.decodeFromJsonElement(PrimitiveString.serializer(), jsonElement)
                jsonObject.containsKey("bytes") -> jsonDecoder.json.decodeFromJsonElement(PrimitiveBytes.serializer(), jsonElement)
                jsonObject.containsKey("expressions") -> jsonDecoder.json.decodeFromJsonElement(Node.serializer(), jsonElement)
                else -> jsonDecoder.json.decodeFromJsonElement(PrimitiveApplication.serializer(), jsonElement)
            }
        }

        override fun serialize(encoder: Encoder, value: MichelineMichelsonV1Expression) {
            when (value) {
                is PrimitiveInt -> encoder.encodeSerializableValue(PrimitiveInt.serializer(), value)
                is PrimitiveString -> encoder.encodeSerializableValue(PrimitiveString.serializer(), value)
                is PrimitiveBytes -> encoder.encodeSerializableValue(PrimitiveBytes.serializer(), value)
                is PrimitiveApplication -> encoder.encodeSerializableValue(PrimitiveApplication.serializer(), value)
                is Node -> encoder.encodeSerializableValue(Node.serializer(), value)
            }
        }
    }
}