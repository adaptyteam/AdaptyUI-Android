@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.UiThread
import com.adapty.Adapty
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.ui.internal.LOG_PREFIX
import com.adapty.ui.internal.LOG_PREFIX_ERROR
import com.adapty.ui.internal.PaywallPresenterFactory
import com.adapty.ui.internal.log
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiEventListener
import com.adapty.ui.listeners.AdaptyUiProductTitleResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE

public class AdaptyPaywallView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        isClickable = true
    }

    private val flowKey = "#${hashCode()}#"

    private val presenter = PaywallPresenterFactory.create(flowKey)

    @get:JvmSynthetic @set:JvmSynthetic
    internal var eventListener: AdaptyUiEventListener? = null
        get() = if (isAttachedToWindow) field else null

    /**
     * Use it to respond to different events happening inside the purchase screen.
     *
     * If the [AdaptyPaywallView] has been created by calling [AdaptyUI.getPaywallView],
     * calling this method is unnecessary.
     *
     * @param[listener] An object that implements the [AdaptyUiEventListener] interface.
     * Also you can extend [AdaptyUiDefaultEventListener] so you don't need to override all the methods.
     */
    public fun setEventListener(listener: AdaptyUiEventListener) {
        eventListener = listener
    }

    /**
     * Should be called only on UI thread
     *
     * If the [AdaptyPaywallView] has been created by calling [AdaptyUI.getPaywallView],
     * calling this method is unnecessary.
     *
     * @param[paywall] An [AdaptyPaywall] object, for which you are trying to display data.
     *
     * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
     * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
     * automatically fetch the required products.
     *
     * @param[viewConfiguration] An [AdaptyViewConfiguration] object containing information
     * about the visual part of the paywall. To load it, use the [Adapty.getViewConfiguration] method.
     *
     * @param[insets] In case the status bar or navigation bar overlap the view, you can pass
     * an [AdaptyPaywallInsets] object.
     * If the status bar doesn't overlap the [AdaptyPaywallView], the [top][AdaptyPaywallInsets.top]
     * value should be 0.
     * If the navigation bar doesn't overlap the [AdaptyPaywallView], the [bottom][AdaptyPaywallInsets.bottom]
     * value should be 0.
     * If none of them do, you can pass [AdaptyPaywallInsets.NONE].
     *
     * @param[productsTitleResolver] In case you want to override display names of the products,
     * you can implement [AdaptyUiProductTitleResolver] and pass your own logic
     * that maps [AdaptyPaywallProduct] to a display name.
     */
    @UiThread
    public fun showPaywall(
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        viewConfiguration: AdaptyViewConfiguration,
        insets: AdaptyPaywallInsets,
        productsTitleResolver: AdaptyUiProductTitleResolver = AdaptyUiProductTitleResolver.DEFAULT,
    ) {
        log(VERBOSE) {
            "$LOG_PREFIX $flowKey showPaywall(template: ${viewConfiguration.templateId}, products: ${products?.size})"
        }
        try {
            clearOldPaywall()
            presenter.showPaywall(
                this,
                viewConfiguration,
                paywall,
                products,
                insets,
                productsTitleResolver,
            )
        } catch (e: Exception) {
            onRenderingError(e)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        presenter.onSizeChanged(this, w, h)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log(VERBOSE) { "$LOG_PREFIX $flowKey onAttachedToWindow" }
    }

    override fun onDetachedFromWindow() {
        clearOldPaywall()
        log(VERBOSE) { "$LOG_PREFIX $flowKey onDetachedFromWindow" }
        super.onDetachedFromWindow()
    }

    private fun clearOldPaywall() {
        presenter.clearOldPaywall()
        removeAllViews()
    }

    private fun onRenderingError(e: Exception) {
        log(ERROR) {
            "$LOG_PREFIX_ERROR $flowKey rendering error: ${e.localizedMessage ?: e.message}"
        }
        eventListener?.onRenderingError(
            adaptyError(
                e,
                "AdaptyUIError: rendering error (${e.localizedMessage ?: e.message})",
                AdaptyErrorCode.DECODING_FAILED,
            ),
            this,
        )
    }
}