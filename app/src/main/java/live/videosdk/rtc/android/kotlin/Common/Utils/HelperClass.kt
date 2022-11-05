package live.videosdk.rtc.android.kotlin.Common.Utils

import android.app.Dialog
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import live.videosdk.rtc.android.kotlin.Common.RobotoFont
import live.videosdk.rtc.android.kotlin.R

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


}
