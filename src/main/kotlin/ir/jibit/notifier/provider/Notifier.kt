package ir.jibit.notifier.provider

import java.util.concurrent.CompletableFuture

/**
 * Defines a contract to handle notifications. The [notify] should be used iff
 * the call to [canNotify] for the same notification returns true.
 *
 * @author Ali Dehghani
 */
interface Notifier {

    /**
     * Determines whether this particular implementation can handle the given notification or not.
     * The implementation should call the [notify] method iff the this method returns true.
     */
    fun canNotify(notification: Notification): Boolean

    /**
     * Performs the actual mechanics of notification processing. This implementation must not throw any
     * exceptions and communicate the function result through an appropriate implementation of
     * [NotificationResponse].
     */
    fun notify(notification: Notification): CompletableFuture<NotificationResponse>
}
