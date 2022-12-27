package live.videosdk.rtc.android.kotlin.GroupCall.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.kotlin.Common.Listener.ParticipantStreamChangeListener
import live.videosdk.rtc.android.kotlin.Common.Utils.HelperClass
import live.videosdk.rtc.android.kotlin.GroupCall.Activity.GroupCallActivity
import live.videosdk.rtc.android.kotlin.GroupCall.Listener.ParticipantChangeListener
import live.videosdk.rtc.android.kotlin.GroupCall.Utils.ParticipantState
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack


class ParticipantViewFragment(var meeting: Meeting?, var position: Int) : Fragment() {
    var participantGridLayout: GridLayout? = null
    var participantChangeListener: ParticipantChangeListener? = null
    var participantState: ParticipantState? = null
    var eglContext: EglBase.Context? = null
    private var participants: List<Participant>? = null
    private var participantListArr: List<List<Participant>>? = null
    var tabLayoutMediator: TabLayoutMediator? = null
    var viewPager2: ViewPager2? = null
    var tabLayout: TabLayout? = null
    private var screenShareFlag = false

    private var popupwindow_obj: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_participant_view, container, false)
        participantGridLayout = view.findViewById(R.id.participantGridLayout)
        viewPager2 = requireActivity().findViewById(R.id.view_pager_video_grid)
        tabLayout = requireActivity().findViewById(R.id.tab_layout_dots)
        eglContext = PeerConnectionUtils.getEglContext()
        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        participantGridLayout!!.setOnTouchListener((activity as GroupCallActivity?)!!.getTouchListener())
        participantChangeListener = object : ParticipantChangeListener {
            override fun onChangeParticipant(participantList: List<List<Participant?>?>?) {
                changeLayout(participantList!!, null)
            }

            override fun onPresenterChanged(screenShare: Boolean) {
                screenShareFlag = screenShare
                updateGridLayout()
                showInGUI(null)
            }

            override fun onSpeakerChanged(
                participantList: List<List<Participant?>?>?,
                activeSpeaker: Participant?
            ) {
                participantList?.let { changeLayout(it, activeSpeaker!!) } ?: activeSpeakerLayout(
                    activeSpeaker
                )
            }
        }
        participantState = ParticipantState.getInstance(meeting!!)
        participantState!!.addParticipantChangeListener(participantChangeListener!!)
    }

    private fun changeLayout(
        participantList: List<List<Participant?>?>?,
        activeSpeaker: Participant?
    ) {
        participantListArr = (participantList as List<List<Participant>>?)!!
        if (position < participantList!!.size) {
            participants = participantList[position]
            if (popupwindow_obj != null && popupwindow_obj!!.isShowing) popupwindow_obj!!.dismiss()
            updateGridLayout()
            showInGUI(activeSpeaker)
            tabLayoutMediator = TabLayoutMediator(
                tabLayout!!, viewPager2!!, true
            ) { tab: TabLayout.Tab?, position: Int ->
                Log.d(
                    "TAG",
                    "onCreate: "
                )
            }
            if (tabLayoutMediator!!.isAttached) {
                tabLayoutMediator!!.detach()
            }
            tabLayoutMediator!!.attach()
            if (participantList!!.size == 1) {
                tabLayout!!.visibility = View.GONE
            } else {
                tabLayout!!.visibility = View.VISIBLE
            }
        }
    }


    override fun onResume() {
        if (position < participantListArr!!.size) {
            val currentParticipants = participantListArr!![position]
            for (i in currentParticipants.indices) {
                val participant = currentParticipants[i]
                if (!participant.isLocal) {
                    for ((_, stream) in participant.streams) {
                        if (stream.kind.equals("video", ignoreCase = true)) stream.resume()
                    }
                }
            }
        }
        super.onResume()
    }

    override fun onPause() {
        if (position < participantListArr!!.size) {
            var otherParticipants: List<Participant> = ArrayList()
            for (i in participantListArr!!.indices) {
                if (position == i) {
                    continue
                }
                otherParticipants = participantListArr!![i]
            }
            for (i in otherParticipants.indices) {
                val participant = otherParticipants[i]
                if (!participant.isLocal) {
                    for ((_, stream) in participant.streams) {
                        if (stream.kind.equals("video", ignoreCase = true)) {
                            stream.pause()
                        }
                    }
                }
            }
        }
        super.onPause()
    }

    // Call where View ready.
    private fun showInGUI(activeSpeaker: Participant?) {
        for (i in participants!!.indices) {
            val participant = participants!![i]
            val participantView = LayoutInflater.from(context)
                .inflate(R.layout.item_participant, participantGridLayout, false)
            val participantCard = participantView.findViewById<CardView>(R.id.ParticipantCard)
            val ivMicStatus = participantView.findViewById<ImageView>(R.id.ivMicStatus)
            //            GifImageView img_participantActiveSpeaker = participantView.findViewById(R.id.img_participantActiveSpeaker);
            if (activeSpeaker == null) {
                participantCard.foreground = null
//                img_participantActiveSpeaker.setVisibility(View.GONE);
//                ivMicStatus.setVisibility(View.VISIBLE);
            } else {
                if (participant.id == activeSpeaker.id) {
                    participantCard.foreground = requireContext().getDrawable(R.drawable.layout_bg)
//                    ivMicStatus.setVisibility(View.GONE);
//                    img_participantActiveSpeaker.setVisibility(View.VISIBLE);
                } else {
                    participantCard.foreground = null
//                    img_participantActiveSpeaker.setVisibility(View.GONE);
//                    ivMicStatus.setVisibility(View.VISIBLE);
                }
            }
            var participantStreamChangeListener: ParticipantStreamChangeListener
            val ivNetwork = participantView.findViewById<ImageView>(R.id.ivNetwork)
            participantStreamChangeListener = object : ParticipantStreamChangeListener {
                override fun onStreamChanged() {
                    if (participant.streams.isEmpty()) {
                        ivNetwork.visibility = View.GONE
                    } else {
                        ivNetwork.visibility = View.VISIBLE
                    }
                }
            }
            ivNetwork.setOnClickListener {
                popupwindow_obj = HelperClass().callStatsPopupDisplay(
                    participant, ivNetwork,
                    requireContext(), false
                )
                popupwindow_obj!!.showAsDropDown(ivNetwork, -350, -85)
            }
            val tvName = participantView.findViewById<TextView>(R.id.tvName)
            val txtParticipantName = participantView.findViewById<TextView>(R.id.txtParticipantName)
            val svrParticipant =
                participantView.findViewById<SurfaceViewRenderer>(R.id.svrParticipantView)
            try {
                svrParticipant.init(eglContext, null)
            } catch (e: Exception) {
                Log.e("Error", "showInGUI: " + e.message)
            }
            if (participant.id == meeting!!.localParticipant.id) {
                tvName.text = "You"
            } else {
                tvName.text = participant.displayName
            }
            txtParticipantName.text = participant.displayName.substring(0, 1)
            for ((_, stream) in participant.streams) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    svrParticipant.visibility = View.VISIBLE
                    val videoTrack = stream.track as VideoTrack
                    videoTrack.addSink(svrParticipant)
                    participantStreamChangeListener.onStreamChanged()
                    break
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    participantStreamChangeListener.onStreamChanged()
                    ivMicStatus.setImageResource(R.drawable.ic_audio_on)
                }
            }
            participant.addEventListener(object : ParticipantEventListener() {
                override fun onStreamEnabled(stream: Stream) {
                    if (stream.kind.equals("video", ignoreCase = true)) {
                        svrParticipant.visibility = View.VISIBLE
                        val videoTrack = stream.track as VideoTrack
                        videoTrack.addSink(svrParticipant)
                        participantStreamChangeListener.onStreamChanged()
                    } else if (stream.kind.equals("audio", ignoreCase = true)) {
                        participantStreamChangeListener.onStreamChanged()
                        ivMicStatus.setImageResource(R.drawable.ic_audio_on)
                    }
                }

                override fun onStreamDisabled(stream: Stream) {
                    if (stream.kind.equals("video", ignoreCase = true)) {
                        val track = stream.track as VideoTrack
                        track?.removeSink(svrParticipant)
                        svrParticipant.clearImage()
                        svrParticipant.visibility = View.GONE
                    } else if (stream.kind.equals("audio", ignoreCase = true)) {
                        ivMicStatus.setImageResource(R.drawable.ic_audio_off)
                    }
                }
            })
            participantGridLayout!!.addView(participantView)
        }
    }

    fun activeSpeakerLayout(activeSpeaker: Participant?) {
        for (j in 0 until participantGridLayout!!.childCount) {
            val participant = participants!![j]
            val participantView = participantGridLayout!!.getChildAt(j)
            val participantCard = participantView.findViewById<CardView>(R.id.ParticipantCard)
//           ImageView ivMicStatus = participantView.findViewById(R.id.ivMicStatus);
//            GifImageView img_participantActiveSpeaker = participantView.findViewById(R.id.img_participantActiveSpeaker);
            if (activeSpeaker == null) {
                participantCard.foreground = null
//                img_participantActiveSpeaker.setVisibility(View.GONE);
//                ivMicStatus.setVisibility(View.VISIBLE);
            } else {
                if (participant.id == activeSpeaker.id) {
                    participantCard.foreground = requireContext().getDrawable(R.drawable.layout_bg)
//                    ivMicStatus.setVisibility(View.GONE);
//                    img_participantActiveSpeaker.setVisibility(View.VISIBLE);
                } else {
                    participantCard.foreground = null
//                    img_participantActiveSpeaker.setVisibility(View.GONE);
//                    ivMicStatus.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    override fun onDestroy() {
        if (participantChangeListener != null) {
            participantState!!.removeParticipantChangeListener(participantChangeListener!!)
        }
        for (i in 0 until participantGridLayout!!.childCount) {
            val view = participantGridLayout!!.getChildAt(i)
            val surfaceViewRenderer =
                view.findViewById<SurfaceViewRenderer>(R.id.svrParticipantView)
            if (surfaceViewRenderer != null) {
                surfaceViewRenderer.clearImage()
                surfaceViewRenderer.visibility = View.GONE
                surfaceViewRenderer.release()
            }
        }
        participantGridLayout!!.removeAllViews()
        super.onDestroy()
    }

    fun updateGridLayout() {
        for (i in 0 until participantGridLayout!!.childCount) {
            val view = participantGridLayout!!.getChildAt(i)
            val surfaceViewRenderer =
                view.findViewById<SurfaceViewRenderer>(R.id.svrParticipantView)
            if (surfaceViewRenderer != null) {
                surfaceViewRenderer.clearImage()
                surfaceViewRenderer.visibility = View.GONE
                surfaceViewRenderer.release()
            }
        }
        participantGridLayout!!.removeAllViews()
        if (screenShareFlag) {
            participantGridLayout!!.columnCount = 2
            participantGridLayout!!.rowCount = 1
        } else {
            if (participants!!.size == 1) {
                participantGridLayout!!.columnCount = 1
                participantGridLayout!!.rowCount = 1
            } else if (participants!!.size == 2) {
                participantGridLayout!!.columnCount = 1
                participantGridLayout!!.rowCount = 2
            } else {
                participantGridLayout!!.columnCount = 2
                participantGridLayout!!.rowCount = 2
            }
        }
    }

    private fun setQuality(quality: String) {
        val participants: Iterator<Participant> = meeting!!.participants.values.iterator()
        for (i in 0 until meeting!!.participants.size) {
            val participant = participants.next()
            participant.quality = quality
        }
    }
}