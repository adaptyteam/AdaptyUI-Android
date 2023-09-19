package com.adapty.ui.internal

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyEligibility.ELIGIBLE
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.utils.AdaptyLogLevel
import java.util.concurrent.Executors

internal fun Float.dp(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    )
}

internal fun WindowManager.getScreenSize(): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        maximumWindowMetrics.bounds.width() to maximumWindowMetrics.bounds.height()
    } else {
        Point().let { outSize ->
            defaultDisplay.getRealSize(outSize)
            outSize.x to outSize.y
        }
    }
}

internal val View.locationOnScreen: IntArray get() = intArrayOf(0,0).also(::getLocationOnScreen)

internal val View.topCoord: Int get() = locationOnScreen[1] - translationY.toInt()

internal val View.bottomCoord: Int get() = topCoord + height

internal fun TextView.setHorizontalGravity(horizontalGravity: Int) {
    gravity = (gravity and Gravity.VERTICAL_GRAVITY_MASK) or horizontalGravity
}

internal fun TextView.setVerticalGravity(verticalGravity: Int) {
    gravity = (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) or verticalGravity
}

internal fun AdaptyViewConfiguration.HorizontalAlign.toGravity() =
    when (this) {
        AdaptyViewConfiguration.HorizontalAlign.LEFT -> Gravity.START
        AdaptyViewConfiguration.HorizontalAlign.CENTER -> Gravity.CENTER_HORIZONTAL
        AdaptyViewConfiguration.HorizontalAlign.RIGHT -> Gravity.END
    }

internal fun AdaptyViewConfiguration.HorizontalAlign.toLayoutAlignment() =
    when (this) {
        AdaptyViewConfiguration.HorizontalAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
        AdaptyViewConfiguration.HorizontalAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
        AdaptyViewConfiguration.HorizontalAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    }

internal fun AdaptyPaywallProduct.hasFreeTrial(): Boolean =
    firstDiscountOfferOrNull()?.paymentMode == PaymentMode.FREE_TRIAL

internal fun AdaptyPaywallProduct.firstDiscountOfferOrNull(): AdaptyProductDiscountPhase? {
    return subscriptionDetails?.let { subDetails ->
        subDetails.introductoryOfferPhases.firstOrNull()?.takeIf { subDetails.introductoryOfferEligibility == ELIGIBLE }
    }
}

private val logExecutor = Executors.newSingleThreadExecutor()

@OptIn(InternalAdaptyApi::class)
internal fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}