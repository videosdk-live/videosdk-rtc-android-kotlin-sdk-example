package live.videosdk.rtc.android.kotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.listeners.PubSubMessageListener
import live.videosdk.rtc.android.model.PubSubPublishOptions

class ChatActivity : AppCompatActivity() {
    private var etmessage: EditText? = null
    private var messageAdapter: MessageAdapter? = null
    private var meeting: Meeting? = null
    private var pubSubMessageListener: PubSubMessageListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //
        etmessage = findViewById(R.id.etMessage)

        //
        meeting = (this.application as MainApplication).meeting

        //
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.title = "Chat"
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        //
        val messageRecyclerView = findViewById<View>(R.id.messageRcv) as RecyclerView
        messageRecyclerView.layoutManager = LinearLayoutManager(applicationContext)

        //
        pubSubMessageListener = PubSubMessageListener { message ->
            messageAdapter!!.addItem(message)
            messageRecyclerView.scrollToPosition(messageAdapter!!.itemCount - 1)
        }

        // Subscribe for 'CHAT' topic
        val pubSubMessageList = meeting!!.pubSub.subscribe("CHAT", pubSubMessageListener)

        //
        messageAdapter =
            MessageAdapter(this, R.layout.item_message_list, pubSubMessageList, meeting!!)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.addOnLayoutChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            messageRecyclerView.scrollToPosition(
                messageAdapter!!.itemCount - 1
            )
        }

        //
        findViewById<View>(R.id.btnSend).setOnClickListener {
            val message = etmessage!!.text.toString()
            if (message != "") {
                val publishOptions = PubSubPublishOptions()
                publishOptions.isPersist = true
                meeting!!.pubSub.publish("CHAT", message, publishOptions)
                etmessage!!.setText("")
            } else {
                Toast.makeText(
                    this@ChatActivity, "Please Enter Message",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        // Unsubscribe for 'CHAT' topic
        meeting!!.pubSub.unsubscribe("CHAT", pubSubMessageListener)
        super.onDestroy()
    }
}