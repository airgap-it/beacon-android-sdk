package it.airgap.beaconsdk.internal.network.data

internal typealias HttpHeader = Pair<String, String>

internal fun AuthorizationHeader(value: String): HttpHeader = HttpHeader("Authorization", value)
internal fun BearerHeader(token: String): HttpHeader = AuthorizationHeader("Bearer $token")