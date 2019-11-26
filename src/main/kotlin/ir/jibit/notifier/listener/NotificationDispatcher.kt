package ir.jibit.notifier.listener

import io.micrometer.core.instrument.MeterRegistry
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
 * @param meterRegistry To register metrics.
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
        ioExecutor.execute { message.process() }
    }

    private fun ByteArray.process() {
        try {
            val parsed = NotificationRequest.parseFrom(this)
            val notification = parsed.toNotification()
            if (notification == null) {
                log.warn("Invalid notification type: {}", parsed.toString())
                return
            }

            val notifier = notifiers?.firstOrNull { it.canNotify(notification) }
            if (notifier == null) {
                log.warn("Couldn't find a notifier capable of handling this particular notification: {}", notification)
                return
            }

            when (val response = notifier.notify(notification)) {
                is SuccessfulNotification -> log.debug("Successfully handled a notification: {}", response)
                is FailedNotification -> log.warn("Failed to deliver a notification: {}", response)
            }
        } catch (ex: Exception) {
            log.warn("Failed to process the notification", ex)
        }
    }

    private fun NotificationRequest.toNotification(): Notification? {
        return when (type) {
            NotificationRequest.Type.SMS -> SmsNotification(message, recipientsList.toSet())
            NotificationRequest.Type.CALL -> CallNotification(message, recipientsList.toSet())
            else -> null
        }
    }
}
