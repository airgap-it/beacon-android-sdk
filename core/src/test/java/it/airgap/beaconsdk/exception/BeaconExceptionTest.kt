package it.airgap.beaconsdk.exception

import org.junit.Test
import kotlin.test.assertEquals

internal class BeaconExceptionTest {

    private val typeAndCauseWithExpected: List<Pair<Pair<BeaconException.Type, Throwable?>, BeaconException>> = listOf(
        Pair(BeaconException.Type.BroadcastError, null) to BeaconException.BroadcastError(),
        Pair(BeaconException.Type.BroadcastError, IllegalStateException()) to BeaconException.BroadcastError(IllegalStateException()),
        Pair(BeaconException.Type.NetworkNotSupported, null) to BeaconException.NetworkNotSupported(),
        Pair(BeaconException.Type.NetworkNotSupported, IllegalStateException()) to BeaconException.NetworkNotSupported(IllegalStateException()),
        Pair(BeaconException.Type.NoAddressError, null) to BeaconException.NoAddressError(),
        Pair(BeaconException.Type.NoAddressError, IllegalStateException()) to BeaconException.NoAddressError(IllegalStateException()),
        Pair(BeaconException.Type.NoPrivateKeyFound, null) to BeaconException.NoPrivateKeyFound(),
        Pair(BeaconException.Type.NoPrivateKeyFound, IllegalStateException()) to BeaconException.NoPrivateKeyFound(IllegalStateException()),
        Pair(BeaconException.Type.NotGranted, null) to BeaconException.NotGranted(),
        Pair(BeaconException.Type.NotGranted, IllegalStateException()) to BeaconException.NotGranted(IllegalStateException()),
        Pair(BeaconException.Type.ParametersInvalid, null) to BeaconException.ParametersInvalid(),
        Pair(BeaconException.Type.ParametersInvalid, IllegalStateException()) to BeaconException.ParametersInvalid(IllegalStateException()),
        Pair(BeaconException.Type.TooManyOperations, null) to BeaconException.TooManyOperations(),
        Pair(BeaconException.Type.TooManyOperations, IllegalStateException()) to BeaconException.TooManyOperations(IllegalStateException()),
        Pair(BeaconException.Type.TransactionInvalid, null) to BeaconException.TransactionInvalid(),
        Pair(BeaconException.Type.TransactionInvalid, IllegalStateException()) to BeaconException.TransactionInvalid(IllegalStateException()),
        Pair(BeaconException.Type.TransactionInvalid, null) to BeaconException.TransactionInvalid(),
        Pair(BeaconException.Type.TransactionInvalid, IllegalStateException()) to BeaconException.TransactionInvalid(IllegalStateException()),
        Pair(BeaconException.Type.Aborted, null) to BeaconException.Aborted(),
        Pair(BeaconException.Type.Aborted, IllegalStateException()) to BeaconException.Aborted(IllegalStateException()),
        Pair(BeaconException.Type.Unknown, null) to BeaconException.Unknown(),
        Pair(BeaconException.Type.Unknown, IllegalStateException()) to BeaconException.Unknown(cause = IllegalStateException()),
    )

    @Test
    fun `creates BeaconException from type`() {
        typeAndCauseWithExpected
            .map { BeaconException.fromType(it.first.first, it.first.second) to it.second }
            .forEach { assertEquals(it.second.toString(), it.first.toString()) }
    }
}