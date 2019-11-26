package ir.jibit.notifier.provider

import ir.jibit.notifier.util.stackTrace

/**
 * Abstract supertype for all notification request types.
 *
 * @author Ali Dehghani
 */
interface Notification

/**
 * Represents the notification processing result. This can either be a [SuccessfulNotification] instance or
 * a [FailedNotification] one. This makes it easy to use pattern matching when handling notification results:
 * ```
 * when(handleNotification()) {
 *     is SuccessfulNotification -> // successful case
 *     is FailedNotification -> // failed case
 * }
 * ```
 *
 * @author Ali Dehghani
 */
sealed class NotificationResponse

/**
 * Encapsulates all necessary information about the failed notification. The client can optionally
 * pass an instance of [Throwable] to specify what exactly went wrong.
 *
 * @author Ali Dehghani
 */
class FailedNotification(val exception: Throwable? = null, val log: String? = null) : NotificationResponse() {

    /**
     * A human friendly representation of a [FailedNotification].
     */
    override fun toString(): String {
        return "Stacktrace: ${exception?.stackTrace() ?: "None"}, Log: ${log ?: "None"}"
    }
}

/**
 * Encapsulates all necessary information about the successful notification. The client can optionally
 * use [log] to store more logs about the response.
 *
 * @author Ali Dehghani
 */
class SuccessfulNotification(val log: String? = null) : NotificationResponse() {

    /**
     * A human friendly representation of a [SuccessfulNotification].
     */
    override fun toString(): String {
        return "Successful: ${log ?: "No Log"}"
    }
}
