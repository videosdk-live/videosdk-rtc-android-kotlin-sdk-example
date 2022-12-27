package live.videosdk.rtc.android.kotlin.Common.Utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.kotlin.Common.RobotoFont
import live.videosdk.rtc.android.kotlin.R
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class HelperClass {

    companion object {

        private var progressDialog: Dialog? = null

        fun setSnackBarStyle(snackbarView: View, textColor: Int) {
            val snackbarTextId = com.google.android.material.R.id.snackbar_text
            val textView = snackbarView.findViewById<View>(snackbarTextId) as TextView
            val params = textView.layoutParams as LinearLayout.LayoutParams
            params.height = 150
            textView.gravity = Gravity.CENTER
            textView.layoutParams = params
            if (textColor == 0) {
                textView.setTextColor(Color.BLACK)
            } else {
                textView.setTextColor(textColor)
            }
            textView.textSize = 15f
            textView.typeface = RobotoFont().getTypeFace(snackbarView.context)
        }

        private fun setViewAndChildrenEnabled(view: View, enabled: Boolean) {
            view.isEnabled = enabled
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    setViewAndChildrenEnabled(child, enabled)
                }
            }
        }

        fun showProgress(view: View) {
            setViewAndChildrenEnabled(view, false)
            progressDialog = Dialog(view.context, R.style.ProgressDialogStyle)
            progressDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            progressDialog!!.setContentView(R.layout.joinmeeting_progress_layout)
            val wmlp = progressDialog!!.window!!.attributes
            wmlp.gravity = Gravity.CENTER or Gravity.CENTER
            wmlp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            wmlp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            progressDialog!!.window!!.setWindowAnimations(R.style.DialogNoAnimation)
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }

        fun hideProgress(view: View) {
            progressDialog!!.dismiss()
            progressDialog!!.cancel()
            setViewAndChildrenEnabled(view, true)
        }

        fun checkParticipantSize(view: View, layout: View?) {
            setViewAndChildrenEnabled(view, false)
            val leaveprogressDialog = Dialog(view.context, R.style.ProgressDialogStyle)
            leaveprogressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            leaveprogressDialog.setContentView(layout!!)
            leaveprogressDialog.window!!.setWindowAnimations(R.style.DialogNoAnimation)
            val wmlp = leaveprogressDialog.window!!.attributes
            wmlp.gravity = Gravity.CENTER or Gravity.CENTER
            wmlp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            wmlp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            leaveprogressDialog.setCanceledOnTouchOutside(false)
            leaveprogressDialog.setCancelable(false)
            leaveprogressDialog.show()
        }
    }

    fun isNullOrEmpty(str: String?): Boolean {
        return "null" == str || "" == str || null == str
    }

    fun callStatsPopupDisplay(
        participant: Participant,
        ivNetwork: ImageView,
        context: Context,
        isScreenShare: Boolean
    ): PopupWindow? {
        val popupWindow = PopupWindow(context)

        // inflate your layout or dynamically add view
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.call_stats, null)
        val linearView = (view as CardView).getChildAt(0)
        val linearLayoutView = (linearView as LinearLayout).getChildAt(0) as LinearLayout
        val childLinearLayoutView = linearLayoutView.getChildAt(0)
        linearLayoutView.findViewById<View>(R.id.btnDismiss)
            .setOnClickListener { popupWindow.dismiss() }
        val tableLayoutView = linearView.getChildAt(1)
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                var audio_stats: JSONObject? = null
                val video_stats: JSONObject

                if (isScreenShare) {
                    video_stats = participant.shareStats
                } else {
                    audio_stats = participant.audioStats
                    video_stats = participant.videoStats
                }

                var score = 0
                if (video_stats != null) score =
                    getQualityScore(video_stats) else if (audio_stats != null) score =
                    getQualityScore(audio_stats)
                if (score >= 7) {
                    setText(childLinearLayoutView.findViewById(R.id.txtScore), "Good", context)
                    linearLayoutView.setBackgroundColor(Color.parseColor("#3BA55D"))
                    ivNetwork.setImageResource(R.drawable.green_signal)
                } else if (score >= 4) {
                    setText(childLinearLayoutView.findViewById(R.id.txtScore), "Average", context)
                    linearLayoutView.setBackgroundColor(Color.parseColor("#F1CC4A"))
                    ivNetwork.setImageResource(R.drawable.orange_signal)
                } else if (score > 0) {
                    setText(childLinearLayoutView.findViewById(R.id.txtScore), "Poor", context)
                    linearLayoutView.setBackgroundColor(Color.parseColor("#FF5D5D"))
                    ivNetwork.setImageResource(R.drawable.red_signal)
                }
                var audio_latency = "-"
                var video_latency = "-"
                var audio_jitter = "-"
                var video_jitter = "-"
                var audio_packetLoss = "-"
                var video_packetLoss = "-"
                var audio_bitrate = "-"
                var video_bitrate = "-"
                var video_frameRate = "-"
                var video_resolution = "-"
                var audio_codec = "-"
                var video_codec = "-"
                try {
                    if (audio_stats != null) {
                        audio_latency = if (audio_stats.has("rtt")) audio_stats.getInt("rtt")
                            .toString() + " ms " else "-"
                        audio_jitter = if (audio_stats.has("jitter")) String.format(
                            "%.2f",
                            audio_stats.getDouble("jitter")
                        ) + " ms " else "-"
                        audio_packetLoss =
                            if (audio_stats.has("packetsLost") && audio_stats.has("totalPackets") && audio_stats.getInt(
                                    "packetsLost"
                                ) > 0 && audio_stats.getInt("totalPackets") > 0
                            ) String.format(
                                "%.2f",
                                audio_stats.getDouble("packetsLost") * 100 / audio_stats.getDouble("totalPackets")
                            ) + "% " else "-"
                        audio_bitrate = if (audio_stats.has("bitrate")) String.format(
                            "%.2f",
                            audio_stats.getDouble("bitrate")
                        ) + " kb/s " else "-"
                        audio_codec =
                            if (audio_stats.has("codec")) audio_stats.getString("codec") else "-"
                    }
                    if (video_stats != null) {
                        video_latency = if (video_stats.has("rtt")) video_stats.getInt("rtt")
                            .toString() + " ms " else "-"
                        video_jitter = if (video_stats.has("jitter")) String.format(
                            "%.2f",
                            video_stats.getDouble("jitter")
                        ) + " ms " else "-"
                        video_packetLoss =
                            if (video_stats.has("packetsLost") && video_stats.has("totalPackets") && video_stats.getInt(
                                    "packetsLost"
                                ) > 0 && video_stats.getInt("totalPackets") > 0
                            ) String.format(
                                "%.2f",
                                video_stats.getDouble("packetsLost") * 100 / video_stats.getDouble("totalPackets")
                            ) + "% " else "-"
                        video_bitrate = if (video_stats.has("bitrate")) String.format(
                            "%.2f",
                            video_stats.getDouble("bitrate")
                        ) + " kb/s " else "-"
                        video_frameRate =
                            if (video_stats.has("size")) if (video_stats.getJSONObject("size")
                                    .has("framerate")
                            ) video_stats.getJSONObject("size").getInt("framerate")
                                .toString() else "-" else "-"
                        video_resolution =
                            if (video_stats.has("size")) if (video_stats.getJSONObject("size")
                                    .has("width") && video_stats.getJSONObject("size").has("height")
                            ) video_stats.getJSONObject("size").getInt("width")
                                .toString() + "x" + video_stats.getJSONObject("size")
                                .getInt("height").toString() else "-" else "-"
                        video_codec =
                            if (video_stats.has("codec")) video_stats.getString("codec") else "-"
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                val tableLayout = tableLayoutView as TableLayout
                try {
                    for (i in 1 until tableLayout.childCount) {
                        val rowView = tableLayout.getChildAt(i)
                        if (rowView is TableRow) {
                            setText(
                                rowView.findViewById(R.id.audio_latency),
                                audio_latency,
                                context
                            )
                            setText(rowView.findViewById(R.id.audio_jitter), audio_jitter, context)
                            setText(
                                rowView.findViewById(R.id.audio_packetLoss),
                                audio_packetLoss,
                                context
                            )
                            setText(
                                rowView.findViewById(R.id.audio_bitrate),
                                audio_bitrate,
                                context
                            )
                            setText(rowView.findViewById(R.id.audio_codec), audio_codec, context)
                            setText(
                                rowView.findViewById(R.id.video_latency),
                                video_latency,
                                context
                            )
                            setText(rowView.findViewById(R.id.video_jitter), video_jitter, context)
                            setText(
                                rowView.findViewById(R.id.video_packetLoss),
                                video_packetLoss,
                                context
                            )
                            setText(
                                rowView.findViewById(R.id.video_bitrate),
                                video_bitrate,
                                context
                            )
                            setText(
                                rowView.findViewById(R.id.video_frameRate),
                                video_frameRate,
                                context
                            )
                            setText(
                                rowView.findViewById(R.id.video_resolution),
                                video_resolution,
                                context
                            )
                            setText(rowView.findViewById(R.id.video_codec), video_codec, context)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 0, 1000)
        popupWindow.contentView = view
        popupWindow.isFocusable = true
        popupWindow.width = dpToPx(270, context)
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.setBackgroundDrawable(context.resources.getDrawable(R.drawable.linearlayout_style))
        popupWindow.setOnDismissListener { timer.cancel() }
        return popupWindow
    }

    fun dpToPx(dp: Int, context: Context): Int {
        val density = context.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    private fun setText(textView: TextView?, value: String, context: Context) {
        (context as Activity).runOnUiThread {
            if (textView != null) {
                textView.text = value
            }
        }
    }


    private fun getQualityScore(stats: JSONObject): Int {
        var packetLossPercent = 0.0
        var jitter = 0.0
        var rtt = 0
        var score = 100
        try {
            packetLossPercent =
                if (stats.has("packetsLost") && stats.has("totalPackets") && stats.getInt("packetsLost") != 0 && stats.getInt(
                        "totalPackets"
                    ) != 0
                ) stats.getDouble("packetsLost") / stats.getDouble("totalPackets") else 0.0
            rtt = if (stats.has("rtt")) stats.getInt("rtt") else 0
            jitter = if (stats.has("jitter")) stats.getDouble("jitter") else 0.0
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        score -= if (packetLossPercent * 50 > 50) 50 else (packetLossPercent * 50).toInt()
        score -= (if (jitter / 30 * 25 > 25) 25 else jitter / 30 * 25).toInt()
        score -= if (rtt / 300 * 25 > 25) 25 else rtt / 300 * 25
        return score / 10
    }


}
