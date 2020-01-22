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
import ir.jibit.notifier.stubs.Notification.NotificationRequest.Type.CALL
import ir.jibit.notifier.stubs.Notification.NotificationRequest.Type.EMAIL
import ir.jibit.notifier.stubs.Notification.NotificationRequest.Type.SMS
import ir.jibit.notifier.util.logger
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
     * Simply submits the incoming request to the executor and returns immediately. Also, records
     * the amount of time took us to submit the new notification request.
     */
    fun dispatch(message: ByteArray) {
        val timer = Timer.start(meterRegistry)

        ioExecutor.execute {
            meterRegistry.counter("notifier.notifications.received").increment()
            message.process()
        }

        val submittedMetric = Timer
            .builder("notifier.notifications.submitted")
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
            .register(meterRegistry)
        timer.stop(submittedMetric)
    }

    /**
     * Parses the receiving byte array into a [Notification], finds the right implementation of
     * [Notifier] for notification processing and delegates the notification processing task to
     * the that [Notifier].
     *
     * All these operations can terminate successfully or exceptionally. Both situations are going
     * to be recorded as Micrometer metrics.
     */
    private fun ByteArray.process() {
        val sample = Timer.start(meterRegistry)
        var notificationType = "invalid"
        try {
            val result = parseNotification(sample) ?: return
            val notification = result.first
            notificationType = result.second

            val notifier = notification.findHandler(sample, notificationType) ?: return
            notifier.handle(notification, notificationType, sample)
        } catch (e: Exception) {
            sample.stop(handledMetric("failed", e.javaClass.simpleName, notificationType))
            log.error("Failed to process the notification", e)
        }
    }

    /**
     * Reads from the receiving byte array and tries to convert it to appropriate
     * instance of [Notification] class. If it fails to convert the message, then
     * it records the failure using [sample] and returns `null`.
     *
     * Also, on successful attempts, it would return the notification type as the second
     * element of a pair.
     */
    private fun ByteArray.parseNotification(sample: Timer.Sample): Pair<Notification, String>? {
        val parsed = NotificationRequest.parseFrom(this)
        val notification = parsed?.toNotification()
        if (notification == null) {
            sample.stop(handledMetric("failed", "InvalidNotificationType"))
            log.warn("Apparently the notification is not one of SMS, CALL, EMAIL or PUSH")
            return null
        }

        val notificationType = parsed.notificationType.name.toLowerCase()
        return notification to notificationType
    }

    /**
     * Finds a [Notifier] instance capable of handling the receiving notification. If it fails
     * to do so, then it would record the failure as a metric and returns `null`.
     */
    private fun Notification.findHandler(sample: Timer.Sample, notificationType: String): Notifier? {
        val notifier = notifiers?.firstOrNull { it.canNotify(this) }
        if (notifier == null) {
            sample.stop(handledMetric("failed", "NoNotificationHandler", notificationType))
            log.warn("Couldn't find a notifier capable of handling this particular notification: {}", this)
            return null
        }

        return notifier
    }

    /**
     * Will use the receiving notifier to handle the notification.
     */
    private fun Notifier.handle(notification: Notification, notificationType: String, sample: Timer.Sample) {
        notify(notification)
            .handle { ok, exception ->
                if (exception != null) FailedNotification(exception = exception)
                else ok
            }
            .thenAccept {
                when (it) {
                    is SuccessfulNotification -> {
                        sample.stop(handledMetric(type = notificationType))
                        log.debug("Successfully handled a notification: {}", it)
                    }
                    is FailedNotification -> {
                        val reason = it.exception?.javaClass?.simpleName ?: "Unknown"
                        sample.stop(handledMetric("failed", reason, notificationType))
                        log.warn("Failed to deliver a notification: {}", it)
                    }
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
     * @param type Represents the notification type this metrics is related to.
     */
    private fun handledMetric(status: String = "ok", exception: String = "none", type: String = "invalid") =
        Timer.builder("notifier.notifications.handled")
            .tag("status", status)
            .tag("exception", exception)
            .tag("type", type)
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.75, 0.9, 0.95, 0.99)
            .register(meterRegistry)

    /**
     * Converts the receiving Protocol Buffer stub to an appropriate instance of [Notification]. Returns
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
