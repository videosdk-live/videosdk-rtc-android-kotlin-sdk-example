package live.videosdk.rtc.android.kotlin.Common.Activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.kotlin.R

class DeviceAdaptor(
    private val mList: MutableList<String>,
    val clickListener: (String) -> Unit
) :
    RecyclerView.Adapter<DeviceAdaptor.ViewHolder>() {
    private var devices: MutableList<String> = mList

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val label: TextView = ItemView.findViewById(R.id.label)
        val icon: ImageView = ItemView.findViewById(R.id.icon)
        val tickIcon: ImageView = ItemView.findViewById(R.id.checkMark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_items_bottom_sheet, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = devices[position]
        holder.label.text = item

        when (item) {
            "BLUETOOTH" -> holder.icon.setImageResource(R.drawable.baseline_bluetooth_connected_24)
            "WIRED_HEADSET" -> holder.icon.setImageResource(R.drawable.baseline_headphones_24)
            "SPEAKER_PHONE" -> holder.icon.setImageResource(R.drawable.baseline_volume_up_24)
            "EARPIECE" -> holder.icon.setImageResource(R.drawable.baseline_call_24)
        }

        holder.itemView.setOnClickListener {
            clickListener(item)
        }

        val selectedAudioDevice = VideoSDK.getSelectedAudioDevice().label
        holder.tickIcon.visibility =
            if (holder.label.text == selectedAudioDevice) View.VISIBLE else View.GONE


    }



}

