package com.adapty.ui.internal

import android.view.View
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ViewAnchor(
    view: View,
    side: Int,
    otherSide: Int,
    margin: Int,
) {
    var view: View = view
        private set

    var side: Int = side
        private set

    var otherSide: Int = otherSide
        private set

    var margin: Int = margin
        private set

    fun updateView(view: View) {
        this.view = view
    }

    fun update(view: View, side: Int, margin: Int) {
        this.view = view
        this.side = side
        this.margin = margin
    }
}