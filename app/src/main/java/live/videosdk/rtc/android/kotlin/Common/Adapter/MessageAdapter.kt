package live.videosdk.rtc.android.kotlin.Common.Adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.lib.PubSubMessage
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    var context: Context,
    messageList: MutableList<PubSubMessage>,
    meeting: Meeting
) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    private var messageList: MutableList<PubSubMessage> = ArrayList()
    private var meeting: Meeting

    init {
        this.messageList = messageList
        this.meeting = meeting
    }

    fun addItem(pubSubMessage: PubSubMessage) {
        messageList.add(pubSubMessage)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemview: View = LayoutInflater.from(parent.context).inflate(
            R.layout.item_message_list,
            parent, false
        )
        return ViewHolder(itemview)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = messageList[position]
        holder.message.text = item.message
        val dateFormat = SimpleDateFormat("hh:mm a")
        val date = Date(item.timestamp)
        holder.messageTime.text = dateFormat.format(date)
        if (item.senderId == meeting.localParticipant.id) {
            holder.messageLayout.gravity = Gravity.RIGHT
            holder.senderName.text = "You"
        } else {
            holder.messageLayout.gravity = Gravity.LEFT
            holder.senderName.text = item.senderName
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var messageLayout: RelativeLayout
        var senderName: TextView
        var message: TextView
        var messageTime: TextView

        init {
            messageLayout = itemView.findViewById(R.id.messageLayout)
            senderName = itemView.findViewById(R.id.senderName)
            message = itemView.findViewById(R.id.message)
            messageTime = itemView.findViewById(R.id.messageTime)
        }
    }
}
