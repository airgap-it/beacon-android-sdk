package it.airgap.beaconsdk.internal.matrix.data.client

data class MatrixClientMessage<T>(val sender: String, val content: T)