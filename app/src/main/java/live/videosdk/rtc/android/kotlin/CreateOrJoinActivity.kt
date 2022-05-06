package live.videosdk.rtc.android.kotlin

import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.material.snackbar.Snackbar
import org.json.JSONException
import org.json.JSONObject

class CreateOrJoinActivity : AppCompatActivity() {
    private val AUTH_TOKEN = BuildConfig.AUTH_TOKEN
    private val AUTH_URL = BuildConfig.AUTH_URL
    private var etMeetingId: EditText? = null
    private val apiServerUrl = "http://localhost:9000"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_or_join)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "VideoSDK RTC"
        setSupportActionBar(toolbar)

        isNetworkAvailable

        val btnCreate = findViewById<Button>(R.id.btnCreateMeeting)
        val btnJoin = findViewById<Button>(R.id.btnJoinMeeting)
        etMeetingId = findViewById(R.id.etMeetingId)

        //
        btnCreate.setOnClickListener { v: View? -> getToken(null) }

        //
        btnJoin.setOnClickListener { v: View? ->
            val meetingId = etMeetingId!!.getText().toString().trim { it <= ' ' }
            val pattern = Regex("\\w{4}\\-\\w{4}\\-\\w{4}")
            if ("" == meetingId) {
                Toast.makeText(
                    this@CreateOrJoinActivity, "Please enter meeting ID",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!pattern.matches(meetingId)) {
                Toast.makeText(
                    this@CreateOrJoinActivity, "Please enter valid meeting ID",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                getToken(meetingId)
            }
        }
    }

    private val isNetworkAvailable: Boolean
        get() {
            val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = manager.activeNetworkInfo
            val isAvailable = networkInfo != null && networkInfo.isConnected
            if (!isAvailable) {
                Snackbar.make(
                    findViewById(R.id.layout), "No Internet Connection",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            return isAvailable
        }

    private fun isNullOrEmpty(str: String?): Boolean {
        return "null" == str || "" == str || null == str
    }

    private fun getToken(meetingId: String?) {
        if (!isNetworkAvailable) {
            return
        }
        if (!isNullOrEmpty(AUTH_TOKEN) && !isNullOrEmpty(AUTH_URL)) {
            Toast.makeText(
                this@CreateOrJoinActivity,
                "Please Provide only one - either auth_token or auth_url",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!isNullOrEmpty(AUTH_TOKEN)) {
            if (meetingId == null) {
                createMeeting(AUTH_TOKEN)
            } else {
                joinMeeting(AUTH_TOKEN, meetingId)
            }
            return
        }
        if (!isNullOrEmpty(AUTH_URL)) {
            AndroidNetworking.get("$apiServerUrl/get-token")
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        try {
                            val token = response.getString("token")
                            if (meetingId == null) {
                                createMeeting(token)
                            } else {
                                joinMeeting(token, meetingId)
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onError(anError: ANError) {
                        anError.printStackTrace()
                        Toast.makeText(
                            this@CreateOrJoinActivity,
                            anError.errorDetail, Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            return
        }
        Toast.makeText(
            this@CreateOrJoinActivity,
            "Please Provide auth_token or auth_url", Toast.LENGTH_SHORT
        ).show()
    }

    private fun createMeeting(token: String) {
        AndroidNetworking.post("https://api.videosdk.live/v1/meetings")
            .addHeaders("Authorization", token)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val meetingId = response.getString("meetingId")
                        val intent = Intent(this@CreateOrJoinActivity, JoinActivity::class.java)
                        intent.putExtra("token", token)
                        intent.putExtra("meetingId", meetingId)
                        startActivity(intent)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    Toast.makeText(
                        this@CreateOrJoinActivity, anError.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun joinMeeting(token: String, meetingId: String) {
        AndroidNetworking.post("https://api.videosdk.live/v1/meetings/$meetingId")
            .addHeaders("Authorization", token)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val intent = Intent(this@CreateOrJoinActivity, JoinActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("meetingId", meetingId)
                    startActivity(intent)
                    etMeetingId!!.text.clear()
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    Toast.makeText(
                        this@CreateOrJoinActivity, anError.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}

