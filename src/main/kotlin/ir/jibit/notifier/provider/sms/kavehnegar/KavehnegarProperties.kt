package ir.jibit.notifier.provider.sms.kavehnegar

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

/**
 * Encapsulates Kavehnegar related configuration properties.
 */
@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "sms-providers.kavehnegar")
data class KavehnegarProperties(

    /**
     * Represents Kavehnegar token to authorized us for use Kavehnegar API.
     */
    @field:NotBlank(message = "The Kavehnegar API token is required (sms-providers.kavehnegar.token)")
    val token: String? = null,

    /**
     * The Kavehnegar base URL which by default is https://api.kavenegar.com/.
     */
    @field:NotBlank(message = "The Kavehnegar Base URL is required (sms-providers.kavehnegar.base-url)")
    val baseUrl: String? = "http://api.kavenegar.com/",

    /**
     * With that number we send SMS to user.
     */
    @field:NotBlank(message = "The Kavehnegar sender is required (sms-providers.kavehnegar.sender)")
    val sender: String? = null
)
