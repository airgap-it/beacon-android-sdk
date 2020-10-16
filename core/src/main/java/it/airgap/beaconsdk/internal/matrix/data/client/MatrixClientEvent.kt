package it.airgap.beaconsdk.internal.matrix.data.client

sealed class MatrixClientEvent<C> {
    abstract val content: C

    data class Invite(override val content: Content) : MatrixClientEvent<Invite.Content>() {
        data class Content(val roomId: String)
    }

    sealed class Message<M> : MatrixClientEvent<Message.Content<M>>() {
        data class Content<T>(val roomId: String, val message: MatrixClientMessage<T>)

        data class Text(override val content: Content<String>) : MatrixClientEvent.Message<String>()
    }
}