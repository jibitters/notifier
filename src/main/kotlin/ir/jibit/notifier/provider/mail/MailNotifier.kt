package ir.jibit.notifier.provider.mail

import ir.jibit.notifier.provider.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/**
 * Responsible for processing mail notification requests. This implementation will be registered
 * iff a bean of type [JavaMailSender] already exists in application context. To register such a
 * bean, you should configure notifier with `spring.mail.*` configuration properties.
 *
 * @param mailSender Is responsible for sending emails.
 *
 * @author Ali Dehghani
 */
@Component
@ConditionalOnProperty(prefix = "spring.mail", name = ["host"])
class MailNotifier(private val mailSender: JavaMailSender) : Notifier {

    /**
     * Can only handle [MailNotification] requests.
     */
    override fun canNotify(notification: Notification) = notification is MailNotification

    /**
     * Validates the given notification and sends it to the destined recipients.
     */
    override suspend fun notify(notification: Notification): NotificationResponse {
        notification as MailNotification
        val failed = notification.validate()
        if (failed != null) return failed

        return try {
            mailSender.send(notification.toMail())
            SuccessfulNotification()
        } catch (e: Exception) {
            FailedNotification(exception = e)
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
}
