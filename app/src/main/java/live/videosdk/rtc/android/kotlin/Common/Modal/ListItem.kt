package live.videosdk.rtc.android.kotlin.Common.Modal

import android.graphics.drawable.Drawable

class ListItem {
    var itemName: String
        private set
    lateinit var itemIcon: Drawable
        private set
    var isSelected: Boolean
        private set
    var itemDescription: String?
        private set

    constructor(itemName: String, itemIcon: Drawable) {
        this.itemName = itemName
        this.itemIcon = itemIcon
        isSelected = false
        itemDescription = null
    }

    constructor(itemName: String, itemIcon: Drawable?, selected: Boolean) {
        this.itemName = itemName
        if (itemIcon != null) {
            this.itemIcon = itemIcon
        }
        isSelected = selected
        itemDescription = null
    }

    constructor(itemName: String, itemDescription: String?, itemIcon: Drawable) {
        this.itemName = itemName
        this.itemIcon = itemIcon
        isSelected = false
        this.itemDescription = itemDescription
    }
}
