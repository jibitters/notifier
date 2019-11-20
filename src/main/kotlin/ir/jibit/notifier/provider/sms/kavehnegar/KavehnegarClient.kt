package ir.jibit.notifier.provider.sms.kavehnegar

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * A Retrofit client responsible for abstracting the network call to Kavehnegar SMS and call API.
 */
interface KavehnegarClient {

    /**
     * Sends SMS.
     *
     * @param token The API token to access this service.
     * @param receptors The receptors of SMS.
     * @param message The message to send.
     * @param sender The phone number which will be shown as the sender to receptors.
     */
    @POST("/v1/{api-token}/sms/send.json")
    fun sendSms(@Path("api-token") token: String,
                        @Query("receptor") receptors: String?,
                        @Query("message") message: String,
                        @Query("sender") sender: String): Call<String>

    /**
     * Make voice call.
     *
     * @param token The API token to access this service.
     * @param receptors The receptors of voice call.
     * @param message The message to send.
     */
    @POST("/v1/{api-token}/call/maketts.json")
    fun makeCall(@Path("api-token") token: String,
                         @Query("receptor") receptors: String?,
                         @Query("message") message: String): Call<String>
}
