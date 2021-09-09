package it.airgap.beaconsdk.network.data

import it.airgap.beaconsdk.internal.network.data.ApplicationJson
import it.airgap.beaconsdk.internal.network.data.AuthorizationHeader
import it.airgap.beaconsdk.internal.network.data.BearerHeader
import org.junit.Test
import kotlin.test.assertEquals

internal class HttpHeaderTest {

    @Test
    fun `creates authorization header`() {
        val authorizationValue = "value"

        assertEquals(Pair("Authorization", authorizationValue), AuthorizationHeader(authorizationValue))
    }

    @Test
    fun `creates authorization header with Bearer tokens`() {
        val token = "token"

        assertEquals(Pair("Authorization", "Bearer $token"), BearerHeader(token))
    }

    @Test
    fun `creates content type header for json`() {
        assertEquals(Pair("Content-Type", "application/json"), ApplicationJson())
    }
}