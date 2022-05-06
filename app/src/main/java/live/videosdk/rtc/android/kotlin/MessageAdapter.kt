package live.videosdk.rtc.android.kotlin

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.lib.PubSubMessage
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    var context: Context,
    var resource: Int,
    messageList: MutableList<PubSubMessage>,
    meeting: Meeting) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    var messageList: MutableList<PubSubMessage> = ArrayList()
    var meeting: Meeting
    fun addItem(pubSubMessage: PubSubMessage) {
        messageList.add(pubSubMessage)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemview = LayoutInflater.from(parent.context).inflate(
            resource,
            parent, false
        )
        return ViewHolder(itemview)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = messageList[position]
        val params = holder.messageLayout.layoutParams as RelativeLayout.LayoutParams
        holder.message.text = item.message
        val dateFormat = SimpleDateFormat("hh:mm a")
        val date = Date(item.timestamp)
        holder.messageTime.text = dateFormat.format(date)
        if (item.senderId == meeting.localParticipant.id) {
            holder.messageLayout.gravity = Gravity.RIGHT
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE)
            holder.senderName.text = "You"
        } else {
            holder.messageLayout.gravity = Gravity.LEFT
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
            holder.senderName.text = item.senderName
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var messageLayout: LinearLayout = itemView.findViewById(R.id.messageLayout)
        var senderName: TextView = itemView.findViewById(R.id.senderName)
        var message: TextView = itemView.findViewById(R.id.message)
        var messageTime: TextView = itemView.findViewById(R.id.messageTime)
    }

    init {
        this.messageList = messageList
        this.meeting = meeting
    }
}