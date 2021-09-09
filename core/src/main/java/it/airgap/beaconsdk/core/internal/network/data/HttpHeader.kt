package it.airgap.beaconsdk.core.internal.network.data

import it.airgap.beaconsdk.core.network.data.HttpHeader

@Suppress("FunctionName")
public fun AuthorizationHeader(value: String): HttpHeader = HttpHeader("Authorization", value)

@Suppress("FunctionName")
public fun BearerHeader(token: String): HttpHeader = AuthorizationHeader("Bearer $token")

@Suppress("FunctionName")
public fun ApplicationJson(): HttpHeader = HttpHeader("Content-Type", "application/json")