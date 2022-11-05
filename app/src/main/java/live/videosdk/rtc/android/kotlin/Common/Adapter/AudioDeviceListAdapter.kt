package live.videosdk.rtc.android.kotlin.Common.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import live.videosdk.rtc.android.kotlin.Common.Modal.ListItem
import live.videosdk.rtc.android.kotlin.R
import java.util.ArrayList

class AudioDeviceListAdapter(context: Context, resource: Int, audioDeviceList: ArrayList<ListItem?>) :
    ArrayAdapter<ListItem?>(context, resource, audioDeviceList) {
    internal val context: Context
    private val audioDeviceList: ArrayList<ListItem?>

    init {
        this.audioDeviceList = audioDeviceList
        this.context = context
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.audio_device_list_layout, parent, false)
        val deviceName = rowView.findViewById<TextView>(R.id.tv_device_name)
        val audioDevice: ListItem? = audioDeviceList[position]
        if (audioDevice!!.isSelected) rowView.setBackgroundColor(context.resources.getColor(R.color.md_grey_200))
        deviceName.text = audioDevice.itemName
        return rowView
    }

    override fun getCount(): Int {
        return audioDeviceList.size
    }
}
