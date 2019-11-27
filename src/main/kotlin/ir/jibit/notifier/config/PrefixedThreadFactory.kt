package ir.jibit.notifier.config

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * A Custom [ThreadFactory] implementation with one simple goal: All created threads should
 * have the same prefix in their name!
 *
 * @param prefix The thread name prefix. No need to end with a trailing dash!
 *
 * @author Ali Dehghani
 */
class PrefixedThreadFactory(private val prefix: String) : ThreadFactory {

    /**
     * Will be used to generate unique thread names.
     */
    private val counter = AtomicInteger(1)

    /**
     * Creates a new non-daemon thread with the given prefix.
     */
    override fun newThread(code: Runnable): Thread {
        return Thread(code, "$prefix-${counter.getAndIncrement()}").apply { isDaemon = false }
    }
}
