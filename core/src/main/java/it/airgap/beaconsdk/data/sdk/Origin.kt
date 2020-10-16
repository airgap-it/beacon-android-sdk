package it.airgap.beaconsdk.data.sdk

import kotlinx.serialization.Serializable

@Serializable
sealed class Origin(val type: Type) {
    abstract val id: String

    @Serializable
    data class Website(override val id: String) : Origin(Type.Website) {
        companion object {}
    }

    @Serializable
    data class Extension(override val id: String) : Origin(Type.Extension) {
        companion object {}
    }

    @Serializable
    data class P2P(override val id: String) : Origin(Type.P2P) {
        companion object {}
    }

    companion object {}

    enum class Type {
        Website,
        Extension,
        P2P
    }
}