package live.videosdk.rtc.android.kotlin.Common.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.AdapterView.OnItemClickListener
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import live.videosdk.rtc.android.kotlin.Common.Activity.CreateOrJoinActivity
import live.videosdk.rtc.android.kotlin.R

class CreateOrJoinFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_create_or_join, container, false)
        
        // Mode selection dropdown
        val modeOptions = requireContext().resources.getStringArray(R.array.mode_options)
        
        val modeArrayAdapter: ArrayAdapter<*> =
            ArrayAdapter<Any?>(requireContext(), R.layout.dropdown_item, modeOptions)
        
        val autocompleteModeTV =
            view.findViewById<View>(R.id.autoCompleteModeView) as AutoCompleteTextView
        autocompleteModeTV.setAdapter(modeArrayAdapter)
        // Set high threshold to prevent filtering based on text
        autocompleteModeTV.threshold = 1000
        autocompleteModeTV.setDropDownBackgroundDrawable(
            ResourcesCompat.getDrawable(
                requireContext().resources,
                R.drawable.et_style,
                null
            )
        )
        
        // Preserve the previously selected mode from activity, or use default
        val activityMode = (activity as CreateOrJoinActivity).selectedMode
        autocompleteModeTV.setText(activityMode, false)
        
        // When clicked, reset the adapter to show all options (clear the filter)
        autocompleteModeTV.setOnClickListener {
            autocompleteModeTV.setAdapter(null)
            autocompleteModeTV.setAdapter(modeArrayAdapter)
            autocompleteModeTV.showDropDown()
        }
        
        autocompleteModeTV.onItemClickListener =
            OnItemClickListener { _, _, i, _ ->
                (activity as CreateOrJoinActivity).selectedMode = modeOptions[i]
            }
        
        view.findViewById<View>(R.id.btnCreateMeeting)
            .setOnClickListener { (activity as CreateOrJoinActivity).createMeetingFragment() }
        view.findViewById<View>(R.id.btnJoinMeeting)
            .setOnClickListener { (activity as CreateOrJoinActivity).joinMeetingFragment() }
        // Inflate the layout for this fragment
        return view
    }
}