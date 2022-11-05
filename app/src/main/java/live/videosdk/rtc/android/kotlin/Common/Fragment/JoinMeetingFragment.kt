package live.videosdk.rtc.android.kotlin.Common.Fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import live.videosdk.rtc.android.kotlin.Common.Activity.CreateOrJoinActivity
import live.videosdk.rtc.android.kotlin.Common.Listener.ResponseListener
import live.videosdk.rtc.android.kotlin.Common.Utils.HelperClass
import live.videosdk.rtc.android.kotlin.Common.Utils.NetworkUtils
import live.videosdk.rtc.android.kotlin.GroupCall.Activity.GroupCallActivity
import live.videosdk.rtc.android.kotlin.OneToOneCall.OneToOneCallActivity
import live.videosdk.rtc.android.kotlin.R

class JoinMeetingFragment : Fragment() {

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_join_meeting, container, false)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etMeetingId = view.findViewById<EditText>(R.id.etMeetingId)
        val btnJoin = view.findViewById<Button>(R.id.btnJoin)

        val meetingType = requireContext().resources.getStringArray(R.array.meeting_options)

        val selectedMeetingType = arrayOfNulls<String>(1)

        val arrayAdapter: ArrayAdapter<*> =
            ArrayAdapter<Any?>(requireContext(), R.layout.dropdown_item, meetingType)

        val autocompleteTV =
            view.findViewById<View>(R.id.autoCompleteTextView) as AutoCompleteTextView
        autocompleteTV.setAdapter(arrayAdapter)
        autocompleteTV.setDropDownBackgroundDrawable(
            ResourcesCompat.getDrawable(
                requireContext().resources,
                R.drawable.et_style,
                null
            )
        )

        autocompleteTV.onItemClickListener =
            OnItemClickListener { _, _, i, _ ->
                selectedMeetingType[0] = meetingType[i]
            }

        btnJoin.setOnClickListener { v: View? ->
            val meetingId = etMeetingId!!.text.toString().trim { it <= ' ' }
            val pattern = Regex("\\w{4}-\\w{4}-\\w{4}")
            if ("" == meetingId) {
                Toast.makeText(
                    context, "Please enter meeting ID",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!pattern.matches(meetingId)) {
                Toast.makeText(
                    context, "Please enter valid meeting ID",
                    Toast.LENGTH_SHORT
                ).show()
            } else if ("" == etName.text.toString()) {
                Toast.makeText(context, "Please Enter Name", Toast.LENGTH_SHORT).show()
            } else {
                val networkUtils = NetworkUtils(context)
                if (networkUtils.isNetworkAvailable()) {
                    networkUtils.getToken(object : ResponseListener<String> {
                        override fun onResponse(token: String?) {
                            networkUtils.joinMeeting(
                                token,
                                etMeetingId.text.toString().trim { it <= ' ' },
                                object : ResponseListener<String> {
                                    override fun onResponse(meetingId: String?) {
                                        var intent: Intent? = null
                                        if (!TextUtils.isEmpty(selectedMeetingType[0])) {
                                            intent =
                                                if (selectedMeetingType[0] == "One to One Meeting") {
                                                    Intent(
                                                        activity as CreateOrJoinActivity?,
                                                        OneToOneCallActivity::class.java
                                                    )
                                                } else {
                                                    Intent(
                                                        activity as CreateOrJoinActivity?,
                                                        GroupCallActivity::class.java
                                                    )
                                                }
                                            intent.putExtra("token", token)
                                            intent.putExtra("meetingId", meetingId)
                                            intent.putExtra(
                                                "webcamEnabled",
                                                (activity as CreateOrJoinActivity?)!!.isWebcamEnabled
                                            )
                                            intent.putExtra(
                                                "micEnabled",
                                                (activity as CreateOrJoinActivity?)!!.isMicEnabled
                                            )
                                            intent.putExtra(
                                                "participantName",
                                                etName.text.toString().trim { it <= ' ' })
                                            startActivity(intent)
                                            (activity as CreateOrJoinActivity?)!!.finish()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Please Choose Meeting Type", Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                })
                        }

                    })
                } else {
                    val snackbar = Snackbar.make(
                        view.findViewById(R.id.joinMeetingLayout),
                        "No Internet Connection",
                        Snackbar.LENGTH_LONG
                    )
                    HelperClass.setSnackBarStyle(snackbar.view, 0)
                    snackbar.show()
                }
            }
        }

        return view
    }
}