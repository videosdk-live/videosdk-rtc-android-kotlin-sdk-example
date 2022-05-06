package live.videosdk.rtc.android.kotlin

import android.view.*
import live.videosdk.rtc.android.Meeting
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import org.webrtc.SurfaceViewRenderer
import android.widget.TextView
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.listeners.MeetingEventListener
import org.webrtc.VideoTrack
import java.util.ArrayList

class ParticipantAdapter(meeting: Meeting) : RecyclerView.Adapter<ParticipantAdapter.PeerViewHolder>() {
    private val participants: MutableList<Participant> = ArrayList()
    private var containerHeight = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        containerHeight = parent.height
        return PeerViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_remote_peer, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        val participant = participants[position]
        if (position == 0) {
            holder.btnMenu.visibility = View.INVISIBLE
        }

        //
        val layoutParams = holder.itemView.layoutParams
        layoutParams.height = containerHeight / 3
        holder.itemView.layoutParams = layoutParams

        //
        holder.tvName.text = participant.displayName
        for ((_, stream) in participant.streams) {
            if (stream.kind.equals("video", ignoreCase = true)) {
                holder.svrParticipant.visibility = View.VISIBLE
                val videoTrack = stream.track as VideoTrack
                videoTrack.addSink(holder.svrParticipant)
                break
            } else if (stream.kind.equals("audio", ignoreCase = true)) {
                holder.ivMicStatus.setImageResource(R.drawable.ic_baseline_mic_24)
            }
        }
        participant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    holder.svrParticipant.visibility = View.VISIBLE
                    val videoTrack = stream.track as VideoTrack
                    videoTrack.addSink(holder.svrParticipant)
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    holder.ivMicStatus.setImageResource(R.drawable.ic_baseline_mic_24)
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    val track = stream.track as VideoTrack
                    track?.removeSink(holder.svrParticipant)
                    holder.svrParticipant.clearImage()
                    holder.svrParticipant.visibility = View.GONE
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    holder.ivMicStatus.setImageResource(R.drawable.ic_baseline_mic_off_24)
                }
            }
        })

        //
        holder.btnMenu.setOnClickListener { v: View? -> showPopup(holder, participant) }
    }

    override fun onViewDetachedFromWindow(holder: PeerViewHolder) {
        holder.svrParticipant.release()
        holder.svrParticipant.clearImage()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewAttachedToWindow(holder: PeerViewHolder) {
        val participant = participants[holder.position]
        holder.svrParticipant.init(PeerConnectionUtils.getEglContext(), null)
        for ((_, stream) in participant.streams) {
            if (stream.kind.equals("video", ignoreCase = true)) {
                holder.svrParticipant.visibility = View.VISIBLE
                val videoTrack = stream.track as VideoTrack
                videoTrack.addSink(holder.svrParticipant)
                break
            } else if (stream.kind.equals("audio", ignoreCase = true)) {
                holder.ivMicStatus.setImageResource(R.drawable.ic_baseline_mic_24)
            }
        }
        super.onViewAttachedToWindow(holder)
    }

    private fun showPopup(holder: PeerViewHolder, participant: Participant) {
        val popup = PopupMenu(holder.itemView.context, holder.btnMenu)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_participant, popup.menu)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.remove -> {
                    participant.remove()
                    return@setOnMenuItemClickListener true
                }
                R.id.toggleMic -> {
                    toggleMic(participant)
                    return@setOnMenuItemClickListener true
                }
                R.id.toggleWebcam -> {
                    toggleWebcam(participant)
                    return@setOnMenuItemClickListener true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun toggleMic(participant: Participant) {
        var micEnabled = false
        for ((_, value) in participant.streams) {
            if (value.kind.equals("audio", ignoreCase = true)) {
                micEnabled = true
                break
            }
        }
        if (micEnabled) {
            participant.disableMic()
        } else {
            participant.enableMic()
        }
    }

    private fun toggleWebcam(participant: Participant) {
        var webcamEnabled = false
        for ((_, value) in participant.streams) {
            if (value.kind.equals("video", ignoreCase = true)) {
                webcamEnabled = true
                break
            }
        }
        if (webcamEnabled) {
            participant.disableWebcam()
        } else {
            participant.enableWebcam()
        }
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    class PeerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var svrParticipant: SurfaceViewRenderer = view.findViewById(R.id.svrParticipant)
        var tvName: TextView =view.findViewById(R.id.tvName)
        var btnMenu: ImageButton = view.findViewById(R.id.btnMenu)
        var ivMicStatus: ImageView = view.findViewById(R.id.ivMicStatus)
    }

    init {
        participants.add(meeting.localParticipant)
        meeting.addEventListener(object : MeetingEventListener() {
            override fun onParticipantJoined(participant: Participant) {
                participants.add(participant)
                notifyItemInserted(participants.size - 1)
            }

            override fun onParticipantLeft(participant: Participant) {
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
}