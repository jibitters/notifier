package ir.jibit.notifier.provider.sms.kavehnegar

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import ir.jibit.notifier.provider.SuccessfulNotification
import ir.jibit.notifier.util.Jackson

/**
 * Encapsulates the Kavehnegar response details of SMS and call services.
 *
 * @author Younes Rahimi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KavehnegarResponse(
    val entries: List<Entry>?,
    val `return`: Return
)

fun KavehnegarResponse.toNotification() = SuccessfulNotification(Jackson.toJson(this))

data class Entry(
    val cost: Int,
    val date: Int,
    val message: String,

    @JsonProperty("messageid")
    val messageId: Int,
    val receptor: String,
    val sender: String,
    val status: Int,

    @JsonProperty("statustext")
    val statusText: String?
)

data class Return(
    val message: String,
    val status: Int
)
