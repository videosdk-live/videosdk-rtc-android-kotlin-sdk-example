package live.videosdk.rtc.android.kotlin.Common.Adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Room
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.lib.PubSubMessage
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    var context: Context,
    messageList: MutableList<PubSubMessage>,
    meeting: Room
) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    private var messageList: MutableList<PubSubMessage> = ArrayList()
    private var meeting: Room

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
        val date = parseTimestamp(item.timestamp)
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

    private fun parseTimestamp(raw: String?): Date {
        if (raw.isNullOrEmpty()) return Date()
        raw.toLongOrNull()?.let { return Date(it) }
        val trimmed = raw.replace(Regex("\\.\\d+"), "")
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss")) {
            try {
                return SimpleDateFormat(pattern, Locale.US).parse(trimmed) ?: continue
            } catch (e: Exception) {
            }
        }
        return Date()
    }
}
