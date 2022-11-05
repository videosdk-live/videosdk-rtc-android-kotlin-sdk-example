package live.videosdk.rtc.android.kotlin.GroupCall.Utils

import android.text.TextUtils
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.kotlin.GroupCall.Listener.ParticipantChangeListener
import live.videosdk.rtc.android.listeners.MeetingEventListener

class ParticipantState(meeting: Meeting) {
    var meeting: Meeting? = meeting
    var perPageParticipantSize = 4
    var participantChangeListenerList: MutableList<ParticipantChangeListener> =
        ArrayList()
    private var screenShare = false

    companion object {
        private var participantState: ParticipantState? = null


        fun getInstance(meeting: Meeting): ParticipantState? {
            if (participantState == null) participantState = ParticipantState(meeting)
            return participantState
        }

        fun destroy() {
            participantState = null
        }
    }

    init {
        meeting.addEventListener(object : MeetingEventListener() {
            override fun onMeetingJoined() {
                super.onMeetingJoined()
                for (i in participantChangeListenerList.indices) {
                    participantChangeListenerList[i].onChangeParticipant(getParticipantList())
                }
            }

            override fun onParticipantJoined(participant: Participant) {
                for (i in participantChangeListenerList.indices) {
                    participantChangeListenerList[i].onChangeParticipant(getParticipantList())
                }
            }

            override fun onParticipantLeft(participant: Participant) {
                for (i in participantChangeListenerList.indices) {
                    participantChangeListenerList[i].onChangeParticipant(getParticipantList())
                }
            }

            override fun onPresenterChanged(participantId: String?) {
                super.onPresenterChanged(participantId)
                if (!TextUtils.isEmpty(participantId)) {
                    perPageParticipantSize = 2
                    screenShare = true
                } else {
                    perPageParticipantSize = 4
                    screenShare = false
                }
                for (i in participantChangeListenerList.indices) {
                    participantChangeListenerList[i].onChangeParticipant(getParticipantList())
                    participantChangeListenerList[i].onPresenterChanged(screenShare)
                }
            }
        })
    }

    fun getParticipantList(): List<MutableList<Participant>> {
        val participantListArr: MutableList<MutableList<Participant>> = ArrayList()
        val participants: Iterator<Participant> = meeting!!.participants.values.iterator()
        if (participantListArr.size == 0) {
            val firstPageParticipantArr: MutableList<Participant> = ArrayList()
            firstPageParticipantArr.add(meeting!!.localParticipant)
            participantListArr.add(firstPageParticipantArr)
        }
        for (i in 0 until meeting!!.participants.size) {
            val participantList = participantListArr[participantListArr.size - 1]
            if (participantList.size == perPageParticipantSize) {
                val newParticipantArr: MutableList<Participant> = ArrayList()
                newParticipantArr.add(participants.next())
                participantListArr.add(newParticipantArr)
            } else {
                val participant = participants.next()
                participantList.add(participant)
            }
        }
        return participantListArr
    }

    fun addParticipantChangeListener(listener: ParticipantChangeListener) {
        participantChangeListenerList.add(listener)
        listener.onChangeParticipant(getParticipantList())
        listener.onPresenterChanged(screenShare)
    }

    fun removeParticipantChangeListener(listener: ParticipantChangeListener) {
        participantChangeListenerList.remove(listener)
    }

}