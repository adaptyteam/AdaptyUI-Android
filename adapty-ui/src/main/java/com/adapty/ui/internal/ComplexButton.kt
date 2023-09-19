package com.adapty.ui.internal

import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ComplexButton(
    val bgView: View,
    val textView: TextView?,
    val paddings: Paddings,
) {
    fun setOnClickListener(listener: OnClickListener) {
        bgView.setOnClickListener(listener)
    }

    fun addToViewGroup(viewGroup: ViewGroup) {
        viewGroup.addView(bgView)
        if (textView != null)
            viewGroup.addView(textView)
    }

    class Paddings(val start: Int, val top: Int, val end: Int, val bottom: Int) {
        companion object {
            fun all(value: Int) = Paddings(value, value, value, value)
        }
    }
}