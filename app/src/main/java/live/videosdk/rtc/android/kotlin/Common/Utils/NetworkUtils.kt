package live.videosdk.rtc.android.kotlin.Common.Utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import live.videosdk.rtc.android.kotlin.BuildConfig
import live.videosdk.rtc.android.kotlin.Common.Listener.ResponseListener
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class NetworkUtils(var context: Context?) {
    private var activeMeetingSeconds = 0

    private val AUTH_TOKEN: String = BuildConfig.AUTH_TOKEN
    private val AUTH_URL: String = BuildConfig.AUTH_URL


    fun isNetworkAvailable(): Boolean {
        val manager =
            context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun getToken(responseListener: ResponseListener<String>) {
        if (!HelperClass().isNullOrEmpty(AUTH_TOKEN) && !HelperClass().isNullOrEmpty(AUTH_URL)) {
            Toast.makeText(
                context,
                "Please Provide only one - either auth_token or auth_url",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!HelperClass().isNullOrEmpty(AUTH_TOKEN)) {
            responseListener.onResponse(AUTH_TOKEN)
            return
        }
        if (!HelperClass().isNullOrEmpty(AUTH_URL)) {
            AndroidNetworking.get("$AUTH_URL/get-token")
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        try {
                            val token = response.getString("token")
                            responseListener.onResponse(token)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onError(anError: ANError) {
                        anError.printStackTrace()
                        Toast.makeText(
                            context,
                            anError.errorDetail, Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            return
        }
        Toast.makeText(
            context,
            "Please Provide auth_token or auth_url", Toast.LENGTH_SHORT
        ).show()
    }

    fun createMeeting(token: String?, meetingEventListener: ResponseListener<String>) {
        AndroidNetworking.post("https://api.videosdk.live/v2/rooms")
            .addHeaders("Authorization", token)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val meetingId = response.getString("roomId")
                        meetingEventListener.onResponse(meetingId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    val errorRes = JSONObject(anError.errorBody)
                    Toast.makeText(
                        context, errorRes.getString("error"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    fun joinMeeting(token: String?, roomId: String, meetingEventListener: ResponseListener<String>) {
        AndroidNetworking.get("https://api.videosdk.live/v2/rooms/validate/$roomId")
            .addHeaders("Authorization", token)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    meetingEventListener.onResponse(roomId)
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    val errorRes = JSONObject(anError.errorBody)
                    Toast.makeText(
                        context, errorRes.getString("error"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    fun fetchMeetingTime(meetingId: String, token: String?, responseListener: ResponseListener<Int>) {
        AndroidNetworking.get("https://api.videosdk.live/v2/sessions/?roomId=$meetingId")
            .addHeaders("Authorization", token)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val jsonArray = response["data"] as JSONArray
                        val startMeetingTime = jsonArray.getJSONObject(0)["start"].toString()
                        var startMeetingDate: Date? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startMeetingDate = Date.from(Instant.parse(startMeetingTime))
                        }
                        val currentTime = Calendar.getInstance().time
                        val difference = currentTime.time - startMeetingDate!!.time
                        activeMeetingSeconds =
                            Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds(difference))
                        responseListener.onResponse(activeMeetingSeconds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    Toast.makeText(
                        context, anError.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


}
