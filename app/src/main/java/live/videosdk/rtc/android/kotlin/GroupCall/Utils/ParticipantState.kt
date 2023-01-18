package live.videosdk.rtc.android.kotlin.GroupCall.Utils

import android.text.TextUtils
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.kotlin.Common.Utils.HelperClass
import live.videosdk.rtc.android.kotlin.GroupCall.Listener.ParticipantChangeListener
import live.videosdk.rtc.android.listeners.MeetingEventListener

class ParticipantState internal constructor(var meeting: Meeting) {
    var perPageParticipantSize = 4
    var participantChangeListenerList: MutableList<ParticipantChangeListener> =
        ArrayList<ParticipantChangeListener>()
    private var screenShare = false
    private var activeSpeakerParticipantList: List<MutableList<Participant>>? = null
    private var participantsArr: List<MutableList<Participant>>? = null

    init {
        meeting.addEventListener(object : MeetingEventListener() {
            override fun onMeetingJoined() {
                super.onMeetingJoined()
                for (i in participantChangeListenerList.indices) {
                    participantChangeListenerList[i].onChangeParticipant(participantList)
                }
            }

            override fun onParticipantJoined(participant: Participant) {
                for (i in participantChangeListenerList.indices) {
                    participantChangeListenerList[i].onChangeParticipant(participantList)
                }
            }

            override fun onParticipantLeft(participant: Participant) {
                for (i in participantChangeListenerList.indices) {
                    participantChangeListenerList[i].onChangeParticipant(participantList)
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
                    participantChangeListenerList[i].onChangeParticipant(participantList)
                    participantChangeListenerList[i].onPresenterChanged(screenShare)
                }
            }

            override fun onSpeakerChanged(participantId: String) {
                super.onSpeakerChanged(participantId)
                var updateGrid = true
                var activeSpeaker: Participant? = null
                if (!HelperClass().isNullOrEmpty(participantId)) {
                    activeSpeaker = if (meeting.localParticipant.id == participantId) {
                        meeting.localParticipant
                    } else {
                        meeting.participants[participantId]
                    }
                    val participants: List<Participant>
                    if (activeSpeakerParticipantList == null) {
                        participants = participantList[0]
                    } else {
                        if (activeSpeakerParticipantList != participantsArr) {
                            activeSpeakerParticipantList = participantsArr
                        }
                        participants = activeSpeakerParticipantList!![0]
                    }
                    for (j in participants.indices) {
                        val participant = participants[j]
                        if (participant.id == participantId) {
                            updateGrid = false
                            break
                        }
                    }
                    if (updateGrid) {
                        activeSpeakerParticipantList =
                            getActiveSpeakerParticipantList(activeSpeaker)
                        participantsArr = activeSpeakerParticipantList
                    }
                } else {
                    updateGrid = false
                }
                for (i in participantChangeListenerList.indices) {
                    if (updateGrid) participantChangeListenerList[i].onSpeakerChanged(
                        activeSpeakerParticipantList,
                        activeSpeaker
                    ) else participantChangeListenerList[i].onSpeakerChanged(null, activeSpeaker)
                }
            }
        })
    }

    val participantList: List<MutableList<Participant>>
        get() {
            val participantListArr: MutableList<MutableList<Participant>> = ArrayList()
            val participants: Iterator<Participant> = meeting.participants.values.iterator()
            if (participantListArr.size == 0) {
                val firstPageParticipantArr: MutableList<Participant> = ArrayList()
                firstPageParticipantArr.add(meeting.localParticipant)
                participantListArr.add(firstPageParticipantArr)
            }
            for (i in 0 until meeting.participants.size) {
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
            participantsArr = participantListArr
            return participantListArr
        }

    fun getActiveSpeakerParticipantList(activeSpeaker: Participant?): List<MutableList<Participant>> {
        val participantListArr: MutableList<MutableList<Participant>> = ArrayList()
        val participants: Iterator<Participant> = meeting.participants.values.iterator()
        if (participantListArr.size == 0) {
            val firstPageParticipantArr: MutableList<Participant> = ArrayList()
            firstPageParticipantArr.add(meeting.localParticipant)
            if (activeSpeaker != null && meeting.localParticipant.id != activeSpeaker.id) {
                firstPageParticipantArr.add(activeSpeaker)
            }
            participantListArr.add(firstPageParticipantArr)
        }
        for (i in 0 until meeting.participants.size) {
            val participantList = participantListArr[participantListArr.size - 1]
            if (participantList.size == perPageParticipantSize) {
                val newParticipantArr: MutableList<Participant> = ArrayList()
                val participant = participants.next()
                if (!(activeSpeaker != null && participant.id == activeSpeaker.id)) {
                    newParticipantArr.add(participant)
                }
                participantListArr.add(newParticipantArr)
            } else {
                val participant = participants.next()
                if (!(activeSpeaker != null && participant.id == activeSpeaker.id)) {
                    participantList.add(participant)
                }
            }
        }
        return participantListArr
    }

    fun addParticipantChangeListener(listener: ParticipantChangeListener) {
        participantChangeListenerList.add(listener)
        listener.onChangeParticipant(participantList)
        listener.onPresenterChanged(screenShare)
        listener.onSpeakerChanged(null, null)
    }

    fun removeParticipantChangeListener(listener: ParticipantChangeListener) {
        participantChangeListenerList.remove(listener)
    }

    companion object {
        private const val TAG = "ParticipantState"
        private var participantState: ParticipantState? = null

        // static method to create instance of Singleton class
        fun getInstance(meeting: Meeting): ParticipantState? {
            if (participantState == null) participantState = ParticipantState(meeting)
            return participantState
        }

        fun destroy() {
            participantState = null
        }
    }
}