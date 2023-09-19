package com.adapty.ui.internal

import android.view.View
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.min

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallScreen(
    val contentContainer: ConstraintLayout,
    val purchaseButton: ComplexButton,
    val productViewsBundles: List<ProductViewsBundle>,
    val loadingView: View,
    val props: Props,
) {

    class Props {
        var paywallViewSizeChangeConsumed = false
        var contentSizeChangeConsumed = false

        val areConsumed get() = paywallViewSizeChangeConsumed && contentSizeChangeConsumed
    }

    fun onSizeChanged(w: Int, h: Int) {
        props.paywallViewSizeChangeConsumed = false
        val padding = ((min(w, h) - LOADING_INDICATOR_WIDTH_DP.dp(loadingView.context)) / 2).toInt()
        loadingView.setPadding(padding, padding, padding, padding)
    }

    fun toggleLoadingView(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }
}