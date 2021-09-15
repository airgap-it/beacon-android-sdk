package it.airgap.beaconsdk.core.internal.network.data

import androidx.annotation.RestrictTo
import it.airgap.beaconsdk.core.network.data.HttpHeader

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("FunctionName")
public fun AuthorizationHeader(value: String): HttpHeader = HttpHeader("Authorization", value)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("FunctionName")
public fun BearerHeader(token: String): HttpHeader = AuthorizationHeader("Bearer $token")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("FunctionName")
public fun ApplicationJson(): HttpHeader = HttpHeader("Content-Type", "application/json")