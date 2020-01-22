package ir.jibit.notifier.provider.mail

import ir.jibit.notifier.provider.FailedNotification
import ir.jibit.notifier.provider.Notification
import ir.jibit.notifier.provider.NotificationResponse
import ir.jibit.notifier.provider.Notifier
import ir.jibit.notifier.provider.SuccessfulNotification
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ExecutorService
import java.util.function.BiFunction

/**
 * Responsible for processing mail notification requests. This implementation will be registered
 * iff a bean of type [JavaMailSender] already exists in application context. To register such a
 * bean, you should configure notifier with `spring.mail.*` configuration properties.
 *
 * @param mailSender Is responsible for sending emails.
 * @param ioExecutor Would be used to submit mail request asynchronously.
 *
 * @author Ali Dehghani
 */
@Component
@ConditionalOnProperty(prefix = "spring.mail", name = ["host"])
class MailNotifier(private val mailSender: JavaMailSender, val ioExecutor: ExecutorService) : Notifier {

    /**
     * Can only handle [MailNotification] requests.
     */
    override fun canNotify(notification: Notification) = notification is MailNotification

    /**
     * Validates the given notification and sends it to the destined recipients.
     */
    override fun notify(notification: Notification): CompletableFuture<NotificationResponse> {
        notification as MailNotification
        val failed = notification.validate()
        if (failed != null) return completedFuture(failed)

        return runAsync(Runnable { mailSender.send(notification.toMail()) }, ioExecutor)
            .handleIo { _, exception ->
                if (exception != null) FailedNotification(exception = exception)
                else SuccessfulNotification()
            }
    }

    /**
     * Validates the receiving mail notification. Returns an instance of [FailedNotification] when
     * the email request is not valid. Returns `null` otherwise.
     */
    private fun MailNotification.validate(): FailedNotification? {
        if (recipients.isEmpty()) return FailedNotification(log = "Email should have at least one recipient")
        if (subject.isBlank()) return FailedNotification(log = "Email should have a valid subject")
        if (body.isBlank()) return FailedNotification(log = "Email should have a valid body")

        return null
    }

    /**
     * Adapts the receiving [MailNotification] to an instance of [SimpleMailMessage].
     */
    private fun MailNotification.toMail(): SimpleMailMessage {
        val mail = SimpleMailMessage()
        mail.setSubject(subject)
        mail.setText(body)
        mail.setTo(*recipients.toTypedArray())
        if (cc.isNotEmpty()) mail.setCc(*cc.toTypedArray())
        if (bcc.isNotEmpty()) mail.setBcc(*bcc.toTypedArray())
        if (sender != null) mail.setFrom(sender)

        return mail
    }

    private fun <T, U> CompletableFuture<T>.handleIo(fn: (T, Throwable?) -> U): CompletableFuture<U> =
        handleAsync(BiFunction { ok, failure -> fn(ok, failure) }, ioExecutor)
}
