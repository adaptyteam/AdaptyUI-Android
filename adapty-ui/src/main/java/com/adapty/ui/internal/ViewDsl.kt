package com.adapty.ui.internal

import android.graphics.Bitmap
import android.graphics.Outline
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

internal fun ViewGroup.content(block: ViewGroup.() -> Unit) {
    block(this)
}

internal fun ViewGroup.imageView(block: ImageView.() -> Unit): ImageView {
    val imageView = ImageView(context)
    imageView.block()
    addView(imageView)
    return imageView
}

internal fun ViewGroup.textView(block: TextView.() -> Unit): TextView {
    val textView = TextView(context)
    textView.block()
    addView(textView)
    return textView
}

internal fun ViewGroup.serviceButton(block: TextView.() -> Unit): TextView {
    return textView {
        id = View.generateViewId()
        val verticalPadding = SERVICE_BUTTON_VERTICAL_PADDING_DP.dp(context).toInt()
        setPadding(paddingLeft, verticalPadding, paddingRight, verticalPadding)
        gravity = Gravity.CENTER
        textSize = 14f
        includeFontPadding = false
        setSingleLine()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        setBackgroundFromAttr(android.R.attr.selectableItemBackground)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    view.height,
                    SERVICE_BUTTON_OUTLINE_CORNER_RADIUS_DP.dp(context)
                )
            }
        }

        block()
    }
}

internal fun ViewGroup.loadingView(block: ProgressBar.() -> Unit): ProgressBar {
    val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
    progressBar.block()
    addView(progressBar)
    return progressBar
}

internal var ImageView.imageBitmap: Bitmap?
    get() {
        throw RuntimeException("Used only as a property-access setter in DSL")
    }
    set(value) {
        setImageBitmap(value)
    }

internal fun ViewGroup.scrollView(block: ScrollView.() -> Unit): ScrollView {
    val scrollView = ScrollView(context)
    scrollView.block()
    addView(scrollView)
    return scrollView
}

internal fun ViewGroup.constraintLayout(block: ConstraintLayout.() -> Unit): ConstraintLayout {
    val constraintLayout = ConstraintLayout(context)
    constraintLayout.block()
    addView(constraintLayout)
    return constraintLayout
}

internal fun ViewGroup.view(block: View.() -> Unit): View {
    val view = View(context)
    view.block()
    addView(view)
    return view
}