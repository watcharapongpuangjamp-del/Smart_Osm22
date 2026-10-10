package com.example.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TombstoneFailClosedTest {

    @Test
    fun returnsTombstonesWhenReadSucceeds() = runBlocking {
        val expected = setOf("H-1", "P-2")

        val actual = readTombstonesFailClosed("unit test") {
            expected
        }

        assertEquals(expected, actual)
    }

    @Test
    fun throwsInsteadOfTreatingFailedReadAsEmptySet() = runBlocking {
        val rootCause = IllegalStateException("Firestore unavailable")

        try {
            readTombstonesFailClosed<Set<String>>("unit test") {
                throw rootCause
            }
            throw AssertionError("Expected a failed tombstone read to abort synchronization")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("Tombstones"))
            assertSame(rootCause, e.cause)
        }
    }

    @Test
    fun propagatesCoroutineCancellation() = runBlocking {
        val cancellation = CancellationException("test cancelled")

        try {
            readTombstonesFailClosed<Set<String>>("unit test") {
                throw cancellation
            }
            throw AssertionError("Expected coroutine cancellation to propagate")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }
}
