package ir.jibit.notifier.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream

private const val EMPTY_STACKTRACE = ""

/**
 * Converts the given `throwable` to its corresponding full stacktrace.
 *
 * @return The full stacktrace for the given exception.
 */
fun Throwable?.stackTrace(): String {
    if (this == null) return EMPTY_STACKTRACE
    try {
        ByteArrayOutputStream().use { baos ->
            PrintStream(baos).use { stream ->
                printStackTrace(stream)
                return "$message\n$baos"
            }
        }
    } catch (e: IOException) {
        return EMPTY_STACKTRACE
    }
}
