package ir.jibit.notifier.provider.sms.kavehnegar

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Encapsulates Kavehnegar related configuration properties.
 *
 * @author Younes Rahimi
 */
@ConfigurationProperties(prefix = "kavehnegar")
@ConstructorBinding
data class KavehnegarProperties(

    /**
     * Represents Kavehnegar token to authorized us for use Kavehnegar API.
     */
    var token: String,

    /**
     * The Kavehnegar base URL which by default is https://api.kavenegar.com/.
     */
    var baseUrl: String,

    /**
     * With that number we send SMS to user.
     */
    var sender: String
)
