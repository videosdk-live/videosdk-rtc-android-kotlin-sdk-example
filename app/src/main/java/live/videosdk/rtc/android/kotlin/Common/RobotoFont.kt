package live.videosdk.rtc.android.kotlin.Common

import android.content.Context
import android.graphics.Typeface

class RobotoFont {
    private var fromAsset: Typeface? = null

    fun getTypeFace(context: Context): Typeface? {
        if (fromAsset == null) {
            fromAsset = Typeface.createFromAsset(context.assets, "fonts/Roboto-Bold.ttf")
        }
        return fromAsset
    }

    fun getTypeFaceMedium(context: Context): Typeface? {
        if (fromAsset == null) {
            fromAsset = Typeface.createFromAsset(context.assets, "fonts/Roboto-Medium.ttf")
        }
        return fromAsset
    }
}