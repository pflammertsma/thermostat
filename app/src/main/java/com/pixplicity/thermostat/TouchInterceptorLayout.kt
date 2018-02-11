package com.pixplicity.thermostat

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class TouchInterceptorLayout : FrameLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrSet: AttributeSet?) : super(context, attrSet)
    constructor(context: Context?, attrSet: AttributeSet?, defStyleAttr: Int) : super(context, attrSet, defStyleAttr)
    constructor(context: Context?, attrSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrSet, defStyleAttr, defStyleRes)

    var interceptTouchListener: OnTouchListener? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (interceptTouchListener?.onTouch(this, ev) == true) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

}
