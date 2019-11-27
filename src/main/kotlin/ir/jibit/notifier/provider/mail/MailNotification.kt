package ir.jibit.notifier.provider.mail

/**
 * Encapsulates the details for mail notification request.
 *
 * @author Ali Dehghani
 */
class MailNotification(

    /**
     * The email subject.
     */
    val subject: String,

    /**
     * The email body.
     */
    val body: String,

    /**
     * Collection of email addresses we're going to send the email to.
     */
    val recipients: Set<String>,

    /**
     * From whom we're going to send this email.
     */
    val sender: String? = null
)
