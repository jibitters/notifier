package ir.jibit.notifier.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.lang.IllegalArgumentException

/**
 * Unit tests for throwables utility class.
 *
 * @author Ali Dehghani
 */
internal class ThrowablesTest {

    @ParameterizedTest
    @MethodSource("provideExceptions")
    fun `stacktrace -- Given an Exception -- Should Return the Expected Stacktrace`(ex: Exception?, expected: String) {
        assertThat(ex.stackTrace()).contains(expected)
    }

    companion object {

        @JvmStatic fun provideExceptions() = listOf(
            arguments(null, ""),
            arguments(IllegalArgumentException(), "\njava.lang.IllegalArgumentException"),
            arguments(IllegalArgumentException("This is it"), "This is it\njava.lang.IllegalArgumentException")
        )
    }
}
