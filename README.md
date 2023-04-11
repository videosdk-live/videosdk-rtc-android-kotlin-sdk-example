<p align="center">
<img width="400" src="https://static.videosdk.live/videosdk_logo_website_black.png"/>
</p>

---

[![Documentation](https://img.shields.io/badge/Read-Documentation-blue)](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/concept-and-architecture)
[![Firebase](https://img.shields.io/badge/Download%20Android-Firebase-green)](https://appdistribution.firebase.dev/i/99ae2c5db3a7e446)
[![Discord](https://img.shields.io/discord/876774498798551130?label=Join%20on%20Discord)](https://discord.gg/bGZtAbwvab)
[![Register](https://img.shields.io/badge/Contact-Know%20More-blue)](https://app.videosdk.live/signup)

At Video SDK, we’re building tools to help companies create world-class collaborative products with capabilities of live audio/videos, compose cloud recordings/rtmp/hls and interaction APIs

## Demo App

📱 Download the sample Android app here: https://appdistribution.firebase.dev/i/99ae2c5db3a7e446

## Features

- [x] Real-time video and audio conferencing
- [x] Enable/disable camera
- [x] Mute/unmute mic
- [x] Switch between front and back camera
- [x] Change audio device
- [x] Screen share
- [x] Chat
- [x] Raise hand
- [x] Recording
- [x] [External call detection](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/get-notified/external-call-detection-event)

<br/>

## Setup Guide
- Sign up on [VideoSDK](https://app.videosdk.live) and visit [API Keys](https://app.videosdk.live/api-keys) section to get your API key and Secret key.

- Get familiarized with [Authentication and Token](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/authentication-and-token).

<br/>

## Prerequisites
- Development environment requirements:
  - [Java Development Kit](https://www.oracle.com/java/technologies/downloads/)
  - Android Studio 3.0 or later
- A physical or virtual mobile device running Android 5.0 or later
- Valid [Video SDK Account](https://app.videosdk.live/)

<br/>

## Run the Sample Project
### 1. Clone the sample project

Clone the repository to your local environment.

```js
git clone https://github.com/videosdk-live/videosdk-rtc-android-kotlin-sdk-example.git
```

### 2. Modify local.properties

Generate temporary token from [Video SDK Account](https://app.videosdk.live/signup).

```js title="local.properties"
auth_token = "TEMPORARY-TOKEN";
```

### 3. Run the sample app

Run the android app with **Shift+F10** or the **▶ Run** from toolbar.

<br/>

## Key Concepts

- `Meeting` - A Meeting represents Real time audio and video communication.

  **`Note : Don't confuse with Room and Meeting keyword, both are same thing 😃`**

- `Sessions` - A particular duration you spend in a given meeting is a referred as session, you can
  have multiple session of a particular meetingId.
- `Participant` - Participant represents someone who is attending the meeting's
  session, `local partcipant` represents self (You), for this self, other participants
  are `remote participants`.
- `Stream` - Stream means video or audio media content that is either published
  by `local participant` or `remote participants`.

<br/>

## Android Permission

Add all the following permissions to AndroidManifest.xml file.

```
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- Needed to communicate with already-paired Bluetooth devices. (Legacy up to Android 11) -->
    <uses-permission
        android:name="android.permission.BLUETOOTH"
        android:maxSdkVersion="30" />
    <uses-permission
        android:name="android.permission.BLUETOOTH_ADMIN"
        android:maxSdkVersion="30" />

    <!-- Needed to communicate with already-paired Bluetooth devices. (Android 12 upwards)-->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

```

<br/>

## Token Generation
Token is used to create and validate a meeting using API and also initialise a meeting.

🛠️ `Development Environment`:

- For development, you can use temporary token. Visit VideoSDK [dashboard](https://app.videosdk.live/api-keys) to generate temporary token.

🌐 `Production Environment`:

- For production, you have to set up an authentication server to authorize users. Follow our official example repositories to setup authentication server, [videosdk-rtc-api-server-examples](https://github.com/videosdk-live/videosdk-rtc-api-server-examples)

<br/>

## API: Create and Validate meeting
- `create meeting` - Please refer this [documentation](https://docs.videosdk.live/api-reference/realtime-communication/create-room) to create meeting.
- `validate meeting`- Please refer this [documentation](https://docs.videosdk.live/api-reference/realtime-communication/validate-room) to validate the meetingId.

<br/>

## [Initialize a Meeting](https://docs.videosdk.live/android/api/sdk-reference/initMeeting)
1. For meeting initialization, you have to first initialize the `VideoSDK`. You can initialize the `VideoSDK` using `initialize()` method.

```js
  VideoSDK.initialize(context: Context)
```

2. After successfully initialization, you can configure `VideoSDK` by passing token in `config` method

```js
  VideoSDK.config(token: String?)
```

3. After VideoSDK initialization and configuration, you can initialize the meeting using `initMeeting()` method. `initMeeting()` will generate a new `Meeting` class and the initiated meeting will be returned.

```js
  val meeting: Meeting? = VideoSDK.initMeeting(
                            context: Context?,
                            meetingId: String?,
                            name: String?,
                            micEnabled: Boolean,
                            webcamEnabled: Boolean,
                            participantId: String?,
                            customTracks: Map<String?, CustomStreamTrack?>?
                        )
```

<br/>

## [Mute/Unmute Local Audio](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/handling-media/mute-unmute-mic)
```js
// unmute mic
meeting!!.unmuteMic()

// mute mic
meeting!!.muteMic()
```

<br/>

## [Change Audio Device](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/handling-media/change-input-output-device#changing-inputoutput-audio-device)
- The `meeting.mics` function allows a participant to list all of the attached microphones (e.g., Bluetooth and Earphone).

```js
 // get connected mics
 val mics= meeting!!.mics
```

- Local participant can change the audio device using `changeMic(device: AudioDevice?)` method of `meeting` class.

```js
// change mic
meeting!!.changeMic(device: AudioDevice?) 
```

Please consult our documentation [Change Audio Device](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/features/mic-controls#2-change-audio-device) for more infromation.

<br/>

## [Enable/Disable Local Webcam](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/handling-media/on-off-camera)
```js
// enable webcam
meeting!!.enableWebcam()

// disable webcam
meeting!!.disableWebcam()
```

<br/>

## [Switch Local Webcam](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/handling-media/change-input-output-device#changing-camera-input-device)
```js
// switch webcam
meeting!!.changeWebcam()
```

<br/>

## [Chat](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/collaboration-in-meeting/chat-using-pubsub)
The chat feature allows participants to send and receive messages about specific topics to which they have subscribed.

```js
// publish
meeting!!.pubSub.publish(topic: String?, message: String?, options: PubSubPublishOptions) 

// pubSubPublishoptions is an object of PubSubPublishOptions, which provides an option, such as persist, which persists message history for upcoming participants.


//subscribe
val pubSubMessageList = meeting!!.pubSub.subscribe(topic: String, listener: PubSubMessageListener)

//unsubscribe
meeting!!.pubSub.unsubscribe(topic: String, listener: PubSubMessageListener)


// receiving messages
// PubSubMessageListener will be invoked with onMessageReceived(PubSubMessage message)
var pubSubMessageListener: PubSubMessageListener? = PubSubMessageListener { message ->
                                                    Log.d("#message", "onMessageReceived: " + message.message)
                                                    }
```

<br/>

## [Leave or End Meeting](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/setup-call/leave-end-meeting)
```js
// Only one participant will leave/exit the meeting; the rest of the participants will remain.
meeting!!.leave()

// The meeting will come to an end for each and every participant. So, use this function in accordance with your requirements.
meeting!!.end()
```

<br/>

## [Setup MeetingEventListener](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/get-notified/meeting-events)
By implementing `MeetingEventListener`, VideoSDK sends callbacks to the client app whenever there is a change or update in the meeting after a user joins.

```js
  val meetingEventListener: MeetingEventListener = object : MeetingEventListener() {
        override fun onMeetingJoined() {
           // This event will be emitted when a localParticipant(you) successfully joined the meeting.
        }

        override fun onMeetingLeft() {
           // This event will be emitted when a localParticipant(you) left the meeting.
        }

        override fun onParticipantJoined(participant: Participant) {
           // This event will be emitted when a new participant joined the meeting.
           // [participant]: new participant who joined the meeting
        }

        override fun onParticipantLeft(participant: Participant) {
           // This event will be emitted when a joined participant left the meeting.
           // [participant]: participant who left the meeting
        }

        override fun onPresenterChanged(participantId: String?) {
           // This event will be emitted when any participant starts or stops screen sharing.
           // [participantId]: Id of participant who shares the screen.
        }

        override fun onSpeakerChanged(participantId: String?) {
           // This event will be emitted when a active speaker changed.
           // [participantId] : Id of active speaker
        }

        override fun onRecordingStarted() {
           // This event will be emitted when recording of the meeting is started.
        }

        override fun onRecordingStopped() {
           // This event will be emitted when recording of the meeting is stopped.
        }

        override fun onExternalCallStarted() {
           // This event will be emitted when local particpant receive incoming call.
        }

        override fun onMeetingStateChanged(state: String) {
           // This event will be emitted when state of meeting changes.
        }
    }
```

<br/>

## [Setup ParticipantEventListener](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/get-notified/participant-events)
By implementing `ParticipantEventListener`, VideoSDK sends callbacks to the client app whenever a participant's video, audio, or screen share stream is enabled or disabled.

```js
  val participantEventListener: ParticipantEventListener = object : ParticipantEventListener() {
       override fun onStreamEnabled(stream: Stream) {
          // This event will be triggered whenever a participant's video, audio or screen share stream is enabled.
       }

       override fun onStreamDisabled(stream: Stream) {
          // This event will be triggered whenever a participant's video, audio or screen share stream is disabled.
       }
   }

```

If you want to learn more about, read the complete documentation of [Android VideoSDK](https://docs.videosdk.live/android/guide/video-and-audio-calling-api-sdk/concept-and-architecture)

<br/>

## Project Description
<br/>

> **Note :**
>
> - **master** branch: Better UI with One-to-One and Group call experience.
> - **v1-code-sample** branch: Simple UI with Group call experience.

<br/>

### App behaviour with different meeting types

- **One-to-One meeting** - The One-to-One meeting allows 2 participants to join a meeting in the app.

- **Group meeting** - The Group meeting allows 2 or more participants to join a meeting in the app.

<br/>

## Project Structure
We have 3 packages :

1. [`OneToOneCall`](app/src/main/java/live/videosdk/rtc/android/kotlin/OneToOneCall) - OneToOneCall package includes all classes/files related to OneToOne meeting.
2. [`GroupCall`](app/src/main/java/live/videosdk/rtc/android/kotlin/GroupCall) - GroupCall package includes all classes/files related to Group meeting.
3. [`Common`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common) - Common package inclues all the classes/files that are used in both meeting type.

<br/>

### [Common package](app/src/main/java/live/videosdk/rtc/android/kotlin/Common)

**1. Create or Join Meeting**
- [`NetworkUtils.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Utils/NetworkUtils.kt) - This class is used to call the api to generate token,create and validate the meeting.
- [`CreateOrJoinActivity.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Activity/CreateOrJoinActivity.kt) and [`activity_create_or_join.xml`](app/src/main/res/layout/activity_create_or_join.xml)
  - This activity is used to ask permissions to the partcipant,and to initiate webcam and mic status.
  - `CreateOrJoinFragment`,`CreateMeetingFragment`,`JoinMeetingFragment` will be bound to this activity.

- [`CreateOrJoinFragment.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Fragment/CreateOrJoinFragment.kt) and [`fragment_create_or_join.xml`](app/src/main/res/layout/fragment_create_or_join.xml) - This fragment will include

  - `Create Meeting Button` - This button will navigate to `CreateMeetingFragment`.
  - `Join Meeting Button` - This button will navigate to `JoinMeetingFragment`.
  <p align="center">
  <img width="300" src="assets/create_join_fragment.gif"/>
  </p>

- [`CreateMeetingFragment.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Fragment/CreateMeetingFragment.kt) and [`fragment_create_meeting.xml`](app/src/main/res/layout/fragment_create_meeting.xml) -  This fragement will include
  - `Dropdown to select meeting type` - This dropdown will give choice for meeting type.
  - `EditText for ParticipantName` - This edit text will contain name of the participant.
  - `Create Meeting Button` - This button will call api for create a new meeting and navigate to `OneToOneCallActivity` or `GroupCallActivity` according to user choice.
  <p align="center">
  <img width="300" src="assets/create_meeting_fragement.gif"/>
  </p>
- [`JoinMeetingFragment.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Fragment/JoinMeetingFragment.kt) and [`fragment_join_meeting.xml`](app/src/main/res/layout/fragment_join_meeting.xml) - This fragement will include
  - `Dropdown to select meeting type` - This dropdown will give choice for meeting type.
  - `EditText for ParticipantName` - This edit text will contain name of the participant.
  - `EditText for MeetingId` - This edit text will contain the meeting Id that you want to join.
  - `Join Meeting Button` - This button will call api for join meeting with meetingId that you provided and navigate to `OneToOneCallActivity` or `GroupCallActivity` according to user choice.
  <p align="center">
  <img width="300" src="assets/join_meeting_fragement.gif"/>
  </p>


**2. ParticipantList**

- [`ParticipantListAdapter.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Adapter/ParticipantListAdapter.kt),[`layout_participants_list_view.xml`](app/src/main/res/layout/layout_participants_list_view.xml) and [`item_participant_list_layout.xml`](app/src/main/res/layout/item_participant_list_layout.xml) files used to show ParticipantList.
  <p align="center">
  <img width="300" src="assets/participant_list.gif"/>
  </p>

**3. Dialogs**

- **MoreOptions**:
  - [`MoreOptionsListAdapter.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Adapter/MoreOptionsListAdapter.kt) class,[`ListItem.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Modal/ListItem.kt) class and [`more_options_list_layout.xml`](app/src/main/res/layout/more_options_list_layout.xml) files used to show `MoreOptions` dialog.
  <p align="center">
  <img width="300" src="assets/more _options.gif"/>
  </p>
- **AudioDeviceList**:
  - [`AudioDeviceListAdapter.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Adapter/AudioDeviceListAdapter.kt) class,[`ListItem.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Modal/ListItem.kt) class and [`audio_device_list_layout.xml`](app/src/main/res/layout/audio_device_list_layout.xml) files used to show `AudioDeviceList` dialog.
  <p align="center">
  <img width="300" src="assets/mic_output_device.gif"/>
  </p>
- **LeaveOrEndDialog**:
  - [`LeaveOptionListAdapter.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Adapter/LeaveOptionListAdapter.kt) class,[`ListItem.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/Common/Modal/ListItem.kt) class and [`leave_options_list_layout.xml`](app/src/main/res/layout/leave_options_list_layout.xml) files used to show `LeaveOrEndDialog`.
  <p align="center">
  <img width="300" src="assets/leave_meeting.gif"/>
  </p>

<br/>

### [OneToOneCall package](app/src/main/java/live/videosdk/rtc/android/kotlin/OneToOneCall)

- [`OneToOneCallActivity.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/OneToOneCall/OneToOneCallActivity.kt) activity is main activity for One-to-One meeting.

<br/>

### [GroupCall package](app/src/main/java/live/videosdk/rtc/android/kotlin/GroupCall)

- [`GroupCallActivity.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/GroupCall/Activity/GroupCallActivity.kt) activity is main activity for Group meeting.
- [`ParticipantViewFragment.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/GroupCall/Fragment/ParticipantViewFragment.kt), [`ParticipantViewAdapter.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/GroupCall/Adapter/ParticipantViewAdapter.kt),[`ParticipantChangeListener.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/GroupCall/Listener/ParticipantChangeListener.kt) and [`ParticipantState.kt`](app/src/main/java/live/videosdk/rtc/android/kotlin/GroupCall/Utils/ParticipantState.kt) is used to show participants in Grid.

<br/>

## Examples 
### Examples for Conference

- [videosdk-rtc-prebuilt-examples](https://github.com/videosdk-live/videosdk-rtc-prebuilt-examples)
- [videosdk-rtc-javascript-sdk-example](https://github.com/videosdk-live/videosdk-rtc-javascript-sdk-example)
- [videosdk-rtc-react-sdk-examplee](https://github.com/videosdk-live/videosdk-rtc-react-sdk-example)
- [videosdk-rtc-react-native-sdk-example](https://github.com/videosdk-live/videosdk-rtc-react-native-sdk-example)
- [videosdk-rtc-flutter-sdk-example](https://github.com/videosdk-live/videosdk-rtc-flutter-sdk-example)
- [videosdk-rtc-android-java-sdk-example](https://github.com/videosdk-live/videosdk-rtc-android-java-sdk-example)
- [videosdk-rtc-android-kotlin-sdk-example](https://github.com/videosdk-live/videosdk-rtc-android-kotlin-sdk-example)
- [videosdk-rtc-ios-sdk-example](https://github.com/videosdk-live/videosdk-rtc-ios-sdk-example)

### Examples for Live Streaming

- [videosdk-hls-react-sdk-example](https://github.com/videosdk-live/videosdk-hls-react-sdk-example)
- [videosdk-hls-react-native-sdk-example](https://github.com/videosdk-live/videosdk-hls-react-native-sdk-example)
- [videosdk-hls-flutter-sdk-example](https://github.com/videosdk-live/videosdk-hls-flutter-sdk-example)
- [videosdk-hls-android-java-example](https://github.com/videosdk-live/videosdk-hls-android-java-example)
- [videosdk-hls-android-kotlin-example](https://github.com/videosdk-live/videosdk-hls-android-kotlin-example)

<br/>

## Documentation
[Read the documentation](https://docs.videosdk.live/) to start using Video SDK.

<br/>

## Community
- [Discord](https://discord.gg/Gpmj6eCq5u) - To get involved with the Video SDK community, ask questions and share tips.
- [Twitter](https://twitter.com/video_sdk) - To receive updates, announcements, blog posts, and general Video SDK tips.
