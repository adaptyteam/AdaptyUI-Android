package com.adapty.ui.internal

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

internal fun selector(block: StateListDrawable.() -> Unit): Drawable {
    val drawable = StateListDrawable()
    drawable.block()
    return drawable
}

internal fun shape(block: GradientDrawable.() -> Unit): Drawable {
    val drawable = GradientDrawable()
    drawable.block()
    return drawable
}

internal fun StateListDrawable.state(state: Int, block: () -> Drawable): Drawable {
    addState(intArrayOf(state), block())
    return this
}

internal fun GradientDrawable.stroke(block: Stroke.() -> Unit): Drawable {
    val stroke = Stroke()
    stroke.block()
    setStroke(stroke.width, stroke.color)
    return this
}

internal class Stroke(var width: Int = 1, var color: Int = Color.BLACK)

internal fun GradientDrawable.cornerRadii(block: CornerRadii.() -> Unit): Drawable {
    val radii = CornerRadii()
    radii.block()
    cornerRadii = floatArrayOf(
        radii.topLeft,
        radii.topLeft,
        radii.topRight,
        radii.topRight,
        radii.bottomRight,
        radii.bottomRight,
        radii.bottomLeft,
        radii.bottomLeft,
    )
    return this
}

internal class CornerRadii(
    var topLeft: Float = 0f,
    var topRight: Float = 0f,
    var bottomRight: Float = 0f,
    var bottomLeft: Float = 0f
)