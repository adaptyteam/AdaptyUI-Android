package com.adapty.ui.internal

import android.view.View
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout

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
    }

    fun toggleLoadingView(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }
}