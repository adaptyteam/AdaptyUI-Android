package com.adapty.ui.internal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallScrollView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attributeSet, defStyleAttr) {

    private val stickyFooter: MutableCollection<View> = mutableListOf()
    private var stickyBottomCoord: Int? = null
    private var initialBottomCoord: Int? = null

    override fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)

        val stickyBottomCoord = stickyBottomCoord ?: return
        val initialBottomCoord = initialBottomCoord ?: return

        updateCurrentTranslation(stickyFooter, initialBottomCoord, stickyBottomCoord, scrollY)
    }

    fun setFooterInfo(view: View, stickyBottomCoord: Int) {
        setFooterInfo(listOf(view), stickyBottomCoord)
    }

    fun setFooterInfo(complexButton: ComplexButton, stickyBottomCoord: Int) {
        setFooterInfo(listOfNotNull(complexButton.bgView, complexButton.textView), stickyBottomCoord)
    }

    private fun setFooterInfo(views: Collection<View>, stickyBottomCoord: Int) {
        val initialBottomCoord = views.first().bottomCoord - topCoord

        updateCurrentTranslation(views, initialBottomCoord, stickyBottomCoord, scrollY)

        this.stickyFooter.apply { clear(); addAll(views) }
        this.stickyBottomCoord = stickyBottomCoord
        this.initialBottomCoord = initialBottomCoord
    }

    private fun updateCurrentTranslation(
        stickyFooter: Collection<View>,
        initialBottomCoord: Int,
        stickyBottomCoord: Int,
        scrollY: Int,
    ) {
        stickyFooter.forEach { view ->
            view.translationY = (stickyBottomCoord - initialBottomCoord + scrollY).toFloat().coerceAtMost(0f)
        }
    }
}