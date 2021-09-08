package it.airgap.beaconsdk.core.internal.network.data

import it.airgap.beaconsdk.core.network.data.HttpHeader

@Suppress("FunctionName")
internal fun AuthorizationHeader(value: String): HttpHeader = HttpHeader("Authorization", value)

@Suppress("FunctionName")
internal fun BearerHeader(token: String): HttpHeader = AuthorizationHeader("Bearer $token")

@Suppress("FunctionName")
internal fun ApplicationJson(): HttpHeader = HttpHeader("Content-Type", "application/json")