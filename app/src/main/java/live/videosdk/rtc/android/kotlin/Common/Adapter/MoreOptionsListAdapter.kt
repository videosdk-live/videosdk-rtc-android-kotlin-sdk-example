package live.videosdk.rtc.android.kotlin.Common.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import live.videosdk.rtc.android.kotlin.Common.Modal.ListItem
import live.videosdk.rtc.android.kotlin.R

class MoreOptionsListAdapter(context: Context, resource: Int, objects: List<ListItem>) :
    ArrayAdapter<ListItem?>(context, resource, objects) {
    private val moreOptionsList: List<ListItem>
    internal val context: Context

    init {
        moreOptionsList = objects
        this.context = context
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = this.context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.more_options_list_layout, parent, false)
        val itemIcon = rowView.findViewById<ImageView>(R.id.iv_item_icon)
        val itemName = rowView.findViewById<TextView>(R.id.tv_item_name)
        val moreOptions: ListItem = moreOptionsList[position]
        itemName.text = moreOptions.itemName
        itemIcon.setImageDrawable(moreOptions.itemIcon)
        return rowView
    }
}