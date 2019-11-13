package ir.jibit.notifier.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Provides a less horrifying syntax to define a [Logger]. For example, instead of the:
 * ```
 *     val log = LoggerFactory.getLogger(User::class.java)
 * ```
 * You can simply write:
 * ```
 *     val log = logger<User>()
 * ```
 */
inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)
