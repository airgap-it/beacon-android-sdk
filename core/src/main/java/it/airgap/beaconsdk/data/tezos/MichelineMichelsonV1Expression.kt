package it.airgap.beaconsdk.data.tezos

import kotlinx.serialization.Serializable

@Serializable
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

    enum class Primitive {
        DataUnit,
        DataTrue,
        DataFalse,
        DataPair,
        DataLeft,
        DataRight,
        DataSome,
        DataNone,
        DataElt,

        InsDrop,
        InsDup,
        InsSwap,
        InsDig,
        InsPush,
        InsSome,
        InsNone,
        InsUnit,
        InsIfNone,
        InsPair,
        InsCar,
        CDR,
        InsLeft,
        InsRight,
        InsIfLeft,
        InsNil,
        InsCons,
        InsIfCons,
        InsSize,
        InsEmptySet,
        InsEmptyMap,
        InsEmptyBigMap,
        InsMap,
        InsIter,
        InsMem,
        InsGet,
        InsUpdate,
        InsIf,
        InsLoop,
        InsLoopLeft,
        InsLambda,
        InsExec,
        InsDip,
        InsFailWith,
        InsCast,
        InsRename,
        InsConcat,
        InsSlice,
        InsPack,
        InsUnpack,
        InsAdd,
        InsSub,
        InsMul,
        InsEdiv,
        InsAbs,
        InsIsNat,
        InsInt,
        InsNeg,
        InsLsl,
        InsLsr,
        InsOr,
        InsAnd,
        InsXor,
        InsNot,
        InsCompare,
        InsEq,
        InsNeq,
        InsLt,
        InsGt,
        InsLe,
        InsGe,
        InsSelf,
        InsContract,
        InsTransferTokens,
        InsSetDelegate,
        InsCreateAccount,
        InsCreateContract,
        InsImplicitAccount,
        InsNow,
        InsAmount,
        InsBalance,
        InsCheckSignature,
        InsBlake2b,
        InsSha256,
        InsSha512,
        InsHashKey,
        InsStepsToQuota,
        InsSource,
        InsSender,
        InsAddress,
        InsChainId,

        TypeInt,
        TypeNat,
        TypeString,
        TypeBytes,
        TypeMutez,
        TypeBool,
        TypeKeyHash,
        TypeTimestamp,
        TypeAddress,
        TypeKey,
        TypeUnit,
        TypeSignature,
        TypeOption,
        TypeList,
        TypeSet,
        TypeOperation,
        TypeContract,
        TypePair,
        TypeOr,
        TypeLambda,
        TypeMap,
        TypeBigMap,
        TypeChainId
    }
}