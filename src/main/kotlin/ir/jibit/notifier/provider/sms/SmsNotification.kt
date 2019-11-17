package ir.jibit.notifier.provider.sms

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import ir.jibit.notifier.provider.Notification

/**
 * Encapsulates the details for a SMS notification request.
 *
 * @author Younes Rahimi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SmsNotification(

    /**
     * The message to send.
     */
    val message: String,

    /**
     * The receptors of SMS.
     */
    val recipients: Set<String>

) : Notification
