package ir.jibit.notifier.listener

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.Notifier
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.provider.mail.MailNotification
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import ir.jibit.notifier.stubs.Notification.NotificationRequest
import ir.jibit.notifier.stubs.Notification.NotificationRequest.Type.*
import ir.jibit.notifier.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService

/**
 * A simple factory over all [Notifier] implementations which receives all incoming notification
 * requests and dispatches them to appropriate [Notifier] implementations.
 *
 * @param notifiers Collection of all [Notifier] implementations that are registered as Spring beans.
 * @param ioExecutor The executor service responsible for processing notification requests.
 * @param meterRegistry To register and expose metrics about how well the notification handlers are doing.
 *
 * @author Ali Dehghani
 */
@Component
class NotificationDispatcher(@Autowired(required = false) private val notifiers: List<Notifier>?,
                             @Autowired private val ioExecutor: ExecutorService,
                             private val meterRegistry: MeterRegistry) {

    /**
     * The logger.
     */
    private val log = logger<NotificationDispatcher>()

    /**
     * A [CoroutineScope] backed by the [ioExecutor].
     */
    private val ioScope = CoroutineScope(ioExecutor.asCoroutineDispatcher())

    /**
     * Simply submits the incoming request to the executor and returns immediately.
     */
    fun dispatch(message: ByteArray) {
        ioScope.launch {
            meterRegistry.counter("notifier.notifications.received").increment()
            message.process()
        }
    }

    /**
     * Parses the receiving byte array into a [Notification], finds the right implementation of
     * [Notifier] for notification processing and delegates the notification processing task to
     * the that [Notifier].
     *
     * All these operations can terminate successfully or exceptionally. Both situations are going
     * to be recorded as Micrometer metrics.
     */
    private suspend fun ByteArray.process() {
        val sample = Timer.start(meterRegistry)
        try {
            val notification = parseNotification(sample) ?: return
            val notifier = notification.findHandler(sample) ?: return
            notifier.handle(notification, sample)
        } catch (ex: Exception) {
            sample.stop(handledMetric("failed", ex.javaClass.simpleName))
            log.error("Failed to process the notification", ex)
        }
    }

    /**
     * Reads from the receiving byte array and tries to convert it to appropriate
     * instance of [Notification] class. If it fails to convert the message, then
     * it records the failure using [sample] and returns `null`.
     */
    private fun ByteArray.parseNotification(sample: Timer.Sample): Notification? {
        val parsed = NotificationRequest.parseFrom(this)
        val notification = parsed?.toNotification()
        if (notification == null) {
            sample.stop(handledMetric("failed", "InvalidNotificationType"))
            log.warn("Apparently the notification is not one of SMS, CALL, EMAIL or PUSH")
            return null
        }

        return notification
    }

    /**
     * Finds a [Notifier] instance capable of handling the receiving notification. If it fails
     * to do so, then it would record the failure as a metric and returns `null`.
     */
    private fun Notification.findHandler(sample: Timer.Sample): Notifier? {
        val notifier = notifiers?.firstOrNull { it.canNotify(this) }
        if (notifier == null) {
            sample.stop(handledMetric("failed", "NoNotificationHandler"))
            log.warn("Couldn't find a notifier capable of handling this particular notification: {}", this)
            return null
        }

        return notifier
    }

    /**
     * Will use the receiving notifier to handle the notification.
     */
    private suspend fun Notifier.handle(notification: Notification, sample: Timer.Sample) {
        when (val response = notify(notification)) {
            is SuccessfulNotification -> {
                sample.stop(handledMetric())
                log.debug("Successfully handled a notification: {}", response)
            }
            is FailedNotification -> {
                val reason = response.exception?.javaClass?.simpleName ?: "Unknown"
                sample.stop(handledMetric("failed", reason))
                log.warn("Failed to deliver a notification: {}", response)
            }
        }
    }

    /**
     * Publishes a new timer metric about the result of a handled notification.
     *
     * @param status Represents the state of the handled notification. It's either
     *               "ok" or "failed".
     * @param exception If the result is "failed", then represents the reason behind the failure.
     *                  Otherwise, it's always should be equal to "none".
     */
    private fun handledMetric(status: String = "ok", exception: String = "none") =
        Timer.builder("notifier.notifications.handled")
            .tag("status", status)
            .tag("exception", exception)
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.75, 0.9, 0.95, 0.99)
            .register(meterRegistry)

    /**
     * Converts the receding Protocol Buffer stub to an appropriate instance of [Notification]. Returns
     * `null` when the notification type is invalid.
     */
    private fun NotificationRequest.toNotification(): Notification? {
        return when (notificationType) {
            SMS -> SmsNotification(message, recipientList.toSet())
            CALL -> CallNotification(message, recipientList.toSet())
            EMAIL -> MailNotification(subject, body, recipientList.toSet(), sender, ccList.toSet(), bccList.toSet())
            else -> null
        }
    }
}
