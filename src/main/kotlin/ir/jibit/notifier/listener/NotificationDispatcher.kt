package ir.jibit.notifier.listener

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.Notifier
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.provider.sms.CallNotification
import ir.jibit.notifier.provider.sms.SmsNotification
import ir.jibit.notifier.stubs.Notification.NotificationRequest
import ir.jibit.notifier.util.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

/**
 * A simple factory over all [Notifier] implementations which receives all incoming notification
 * requests and dispatches them to appropriate [Notifier] implementations.
 *
 * @param notifiers Collection of all [Notifier] implementations that are registered as Spring beans.
 * @param meterRegistry To register and expose metrics about how well the notification handlers are doing.
 *
 * @author Ali Dehghani
 */
@Component
class NotificationDispatcher(@Autowired(required = false) private val notifiers: List<Notifier>?,
                             private val meterRegistry: MeterRegistry) {

    /**
     * The logger.
     */
    private val log = logger<NotificationDispatcher>()

    /**
     * An executor service responsible for executing actual IO operations.
     */
    private val ioExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)

    /**
     * Simply submits the incoming request to the executor and returns immediately.
     */
    fun dispatch(message: ByteArray) {
        meterRegistry.counter("notifier.notifications.received").increment()
        ioExecutor.execute { message.process() }
    }

    private fun ByteArray.process() {
        val sample = Timer.start(meterRegistry)
        try {
            val parsed = NotificationRequest.parseFrom(this)
            val notification = parsed.toNotification()
            if (notification == null) {
                sample.stop(handledMetric("failed", "InvalidNotificationType"))
                log.warn("Invalid notification type: {}", parsed.toString())
                return
            }

            val notifier = notifiers?.firstOrNull { it.canNotify(notification) }
            if (notifier == null) {
                sample.stop(handledMetric("failed", "NoNotificationHandler"))
                log.warn("Couldn't find a notifier capable of handling this particular notification: {}", notification)
                return
            }

            when (val response = notifier.notify(notification)) {
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
        } catch (ex: Exception) {
            sample.stop(handledMetric("failed", ex.javaClass.simpleName))
            log.warn("Failed to process the notification", ex)
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
            .publishPercentiles(50.0, 75.0, 90.0, 95.0, 99.0)
            .tag("status", status)
            .tag("exception", exception)
            .register(meterRegistry)

    /**
     * Converts the receding Protocol Buffer stub to an appropriate instance of [Notification]. Returns
     * `null` when the notification type is invalid.
     */
    private fun NotificationRequest.toNotification(): Notification? {
        return when (type) {
            NotificationRequest.Type.SMS -> SmsNotification(message, recipientsList.toSet())
            NotificationRequest.Type.CALL -> CallNotification(message, recipientsList.toSet())
            else -> null
        }
    }
}
