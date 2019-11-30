package ir.jibit.notifier.provider.mail

import ir.jibit.notifier.provider.Notification

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
    val sender: String? = null,

    /**
     * Who's gonna be CC-ed?
     */
    val cc: Set<String> = emptySet(),

    /**
     * Who's gonna be BCC-ed?
     */
    val bcc: Set<String> = emptySet()

) : Notification
