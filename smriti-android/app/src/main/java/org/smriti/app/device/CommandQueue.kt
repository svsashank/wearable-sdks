package org.smriti.app.device

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * The ChipletRing drops commands sent too close together: one at a time,
 * about 300 ms apart (vendor rule). Every ring command goes through here.
 */
class CommandQueue(private val gapMs: Long = 300, private val clock: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    private var lastSentAt = 0L

    suspend fun <T> send(timeoutMs: Long = 10_000, block: suspend () -> T): T = mutex.withLock {
        val wait = lastSentAt + gapMs - clock()
        if (wait > 0) delay(wait)
        try {
            withTimeout(timeoutMs) { block() }
        } finally {
            lastSentAt = clock()
        }
    }
}
