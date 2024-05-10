package com.adapty.ui.internal

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyEligibility.ELIGIBLE
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.ui.AdaptyUI
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

internal fun View.addOnPreDrawListener(listener: OnPreDrawListener) {
    if (isAttachedToWindow)
        viewTreeObserver.addOnPreDrawListener(listener)

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View?) {
            viewTreeObserver.addOnPreDrawListener(listener)
        }

        override fun onViewDetachedFromWindow(v: View?) {
            viewTreeObserver.removeOnPreDrawListener(listener)
        }
    })
}

internal fun TextView.setHorizontalGravity(horizontalGravity: Int) {
    gravity = (gravity and Gravity.VERTICAL_GRAVITY_MASK) or horizontalGravity
}

internal fun TextView.setVerticalGravity(verticalGravity: Int) {
    gravity = (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) or verticalGravity
}

internal fun Context.getCurrentLocale() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        resources.configuration.locale
    }

internal fun AdaptyUI.ViewConfiguration.HorizontalAlign.toGravity() =
    when (this) {
        AdaptyUI.ViewConfiguration.HorizontalAlign.LEFT -> Gravity.START
        AdaptyUI.ViewConfiguration.HorizontalAlign.CENTER -> Gravity.CENTER_HORIZONTAL
        AdaptyUI.ViewConfiguration.HorizontalAlign.RIGHT -> Gravity.END
    }

internal fun AdaptyUI.ViewConfiguration.HorizontalAlign.toLayoutAlignment() =
    when (this) {
        AdaptyUI.ViewConfiguration.HorizontalAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
        AdaptyUI.ViewConfiguration.HorizontalAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
        AdaptyUI.ViewConfiguration.HorizontalAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    }

internal val AdaptyUI.ViewConfiguration.Component.Button.Transition.interpolator: TimeInterpolator
    get() = when (interpolatorName) {
        "ease_in_out" -> AccelerateDecelerateInterpolator()
        "ease_in" -> AccelerateInterpolator()
        "ease_out" -> DecelerateInterpolator()
        "linear" -> LinearInterpolator()
        else -> AccelerateDecelerateInterpolator()
    }

internal fun AdaptyPaywallProduct.hasFreeTrial(): Boolean =
    firstDiscountOfferOrNull()?.paymentMode == PaymentMode.FREE_TRIAL

internal fun AdaptyPaywallProduct.firstDiscountOfferOrNull(): AdaptyProductDiscountPhase? {
    return subscriptionDetails?.let { subDetails ->
        subDetails.introductoryOfferPhases.firstOrNull()?.takeIf { subDetails.introductoryOfferEligibility == ELIGIBLE }
    }
}

internal fun <T> List<T>.withProductLayoutOrdering(
    templateConfig: TemplateConfig,
    productBlockType: Products.BlockType,
): List<T> {
    return if (templateConfig.isReverseProductAddingOrder(productBlockType)) reversed() else this
}

private val logExecutor = Executors.newSingleThreadExecutor()

@OptIn(InternalAdaptyApi::class)
internal fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}