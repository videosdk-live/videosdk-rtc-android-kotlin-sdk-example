package live.videosdk.rtc.android.kotlin.Common.Adapter

import android.content.Context
import android.os.Build
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.kotlin.Common.RobotoFont
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener

class ParticipantListAdapter(
    items: ArrayList<Participant>?,
    meeting: Meeting,
    private val context: Context
) :
    RecyclerView.Adapter<ParticipantListAdapter.ViewHolder>() {
    private val participants = ArrayList<Participant>()

    init {
        participants.add(meeting.localParticipant)
        participants.addAll(items!!)
        meeting.addEventListener(object : MeetingEventListener() {
            override fun onParticipantJoined(participant: Participant) {
                super.onParticipantJoined(participant)
                participants.add(participant)
                notifyItemInserted(participants.size - 1)
            }

            override fun onParticipantLeft(participant: Participant) {
                super.onParticipantLeft(participant)
                var pos = -1
                for (i in participants.indices) {
                    if (participants[i].id == participant.id) {
                        pos = i
                        break
                    }
                }
                participants.remove(participant)
                if (pos >= 0) {
                    notifyItemRemoved(pos)
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_participant_list_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val participant = participants[position]
        if (participants[position].isLocal) {
            holder.participantName.text = "You"
        } else {
            holder.participantName.text = participants[position].displayName
        }
        holder.participantNameFirstLetter.text =
            participants[position].displayName.subSequence(0, 1)

        if (getVideoStreamStatus(participant)) {
            holder.camStatus.setImageResource(R.drawable.ic_webcam_on_style)
        }
        if (getAudioStreamStatus(participant)) {
            holder.micStatus.setImageResource(R.drawable.ic_mic_on_style)
        }

        participant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    holder.camStatus.setImageResource(R.drawable.ic_webcam_on_style)
                }
                if (stream.kind.equals("audio", ignoreCase = true)) {
                    holder.micStatus.setImageResource(R.drawable.ic_mic_on_style)
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    holder.camStatus.setImageResource(R.drawable.ic_webcam_off_style)
                }
                if (stream.kind.equals("audio", ignoreCase = true)) {
                    holder.micStatus.setImageResource(R.drawable.ic_mic_off_style)
                }
            }
        })
        if (participant.isLocal) {
            holder.btnParticipantMoreOptions.visibility = View.GONE
        }


        //
        holder.btnParticipantMoreOptions.setOnClickListener {
            showPopup(
                holder,
                participant
            )
        }
    }

    private fun getVideoStreamStatus(participant: Participant): Boolean {
        val webCamOn = booleanArrayOf(false)
        for ((_, stream) in participant.streams) {
            if (stream.kind.equals("video", ignoreCase = true)) {
                webCamOn[0] = true
                break
            }
        }
        return webCamOn[0]
    }

    private fun getAudioStreamStatus(participant: Participant): Boolean {
        val micOn = booleanArrayOf(false)
        for ((_, stream) in participant.streams) {
            if (stream.kind.equals("audio", ignoreCase = true)) {
                micOn[0] = true
                break
            }
        }
        return micOn[0]
    }

    private fun showPopup(holder: ViewHolder, participant: Participant) {
        val popup = PopupMenu(context, holder.btnParticipantMoreOptions)
        popup.menu.add("Remove Participant")
        if (getVideoStreamStatus(participant)) {
            popup.menu.add("Disable Webcam")
        } else {
            popup.menu.add("Enable Webcam")
        }
        if (getAudioStreamStatus(participant)) {
            popup.menu.add("Mute Mic")
        } else {
            popup.menu.add("UnMute Mic")
        }
        popup.setOnMenuItemClickListener { item: MenuItem ->
            if (item.toString() == "Remove Participant") {
                participant.remove()
                return@setOnMenuItemClickListener true
            } else if (item.toString() == "Disable Webcam") {
                participant.disableWebcam()
                return@setOnMenuItemClickListener true
            } else if (item.toString() == "Enable Webcam") {
                participant.enableWebcam()
                return@setOnMenuItemClickListener true
            } else if (item.toString() == "Mute Mic") {
                participant.disableMic()
                return@setOnMenuItemClickListener true
            } else if (item.toString() == "UnMute Mic") {
                participant.enableMic()
                return@setOnMenuItemClickListener true
            }
            false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popup.gravity = Gravity.END
        }
        popup.show()
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var participantName: TextView
        var micStatus: ImageView
        var camStatus: ImageView
        var participantNameFirstLetter: TextView
        var btnParticipantMoreOptions: ImageButton

        init {
            participantName = itemView.findViewById(R.id.participant_Name)
            participantName.typeface = RobotoFont().getTypeFace(participantName.context)
            micStatus = itemView.findViewById(R.id.mic_status)
            camStatus = itemView.findViewById(R.id.cam_status)
            btnParticipantMoreOptions = itemView.findViewById(R.id.btnParticipantMoreOptions)
            participantNameFirstLetter = itemView.findViewById(R.id.participantNameFirstLetter)
        }
    }
}

