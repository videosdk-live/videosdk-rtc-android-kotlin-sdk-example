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
import android.util.Log

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
            Log.d("NetworkUtils", "getToken: Using AUTH_TOKEN from BuildConfig")
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
        Log.d("NetworkUtils", "createMeeting: Calling API with token: ${token?.take(20)}...")
        Log.d("NetworkUtils", "createMeeting: URL = https://api.classplus-prod.videosdk.live/v2/rooms")
        AndroidNetworking.post("https://api.classplus-prod.videosdk.live/v2/rooms")
            .addHeaders("Authorization", token)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Log.d("NetworkUtils", "createMeeting: SUCCESS - Response: $response")
                    try {
                        val meetingId = response.getString("roomId")
                        Log.d("NetworkUtils", "createMeeting: Got meetingId = $meetingId")
                        meetingEventListener.onResponse(meetingId)
                    } catch (e: Exception) {
                        Log.e("NetworkUtils", "createMeeting: Parse error", e)
                        e.printStackTrace()
                    }
                }

                override fun onError(anError: ANError) {
                    Log.e("NetworkUtils", "createMeeting: ERROR - ${anError.errorCode} - ${anError.errorDetail}")
                    Log.e("NetworkUtils", "createMeeting: Error body = ${anError.errorBody}")
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
        AndroidNetworking.get("https://api.classplus-prod.videosdk.live/v2/rooms/validate/$roomId")
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
        AndroidNetworking.get("https://api.classplus-prod.videosdk.live/v2/sessions/?roomId=$meetingId")
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
