package ir.jibit.notifier.provider

/**
 * Abstract superclass for all notification request types.
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
 */
sealed class NotificationResponse

/**
 * Encapsulates all necessary information about the failed notification. The client can optionally
 * pass an instance of [Throwable] to specify what exactly went wrong.
 */
class FailedNotification(val exception: Throwable? = null) : NotificationResponse() {

    /**
     * A human friendly representation of a [FailedNotification].
     */
    override fun toString(): String {
        return "Failed: ${exception?.stackTrace ?: "No Stacktrace"}"
    }
}

/**
 * Encapsulates all necessary information about the successful notification. The client can optionally
 * use [log] to store more logs about the response.
 */
class SuccessfulNotification(val log: String? = null) {

    /**
     * A human friendly representation of a [SuccessfulNotification].
     */
    override fun toString(): String {
        return "Successful: ${log ?: "No Log"}"
    }
}
