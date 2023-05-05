package com.adapty.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.annotation.UiThread
import com.adapty.Adapty
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiEventListener

public object AdaptyUI {

    /**
     * Right after receiving [AdaptyViewConfiguration], you can create the corresponding
     * [AdaptyPaywallView] to display it afterwards.
     *
     * This method should be called only on UI thread.
     *
     * @param[activity] An [Activity] instance.
     *
     * @param[paywall] An [AdaptyPaywall] object, for which you are trying to get an [AdaptyPaywallView].
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
     * @param[eventListener] An object that implements the [AdaptyUiEventListener] interface.
     * Use it to respond to different events happening inside the purchase screen.
     * If you pass `null`, an [AdaptyUiDefaultEventListener] instance will be used.
     * Also you can extend [AdaptyUiDefaultEventListener] so you don't need to override all the methods.
     *
     * @return An [AdaptyPaywallView] object, representing the requested paywall screen.
     */
    @JvmStatic
    @JvmOverloads
    @UiThread
    public fun getPaywallView(
        activity: Activity,
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        viewConfiguration: AdaptyViewConfiguration,
        insets: AdaptyPaywallInsets,
        eventListener: AdaptyUiEventListener? = null,
    ): AdaptyPaywallView {
        return AdaptyPaywallView(activity).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            eventListener?.let(::setEventListener)
            showPaywall(
                paywall,
                products,
                viewConfiguration,
                insets,
            )
        }
    }

    public sealed class Event {
        /**
         * This event occurs when a successful restore is made.
         */
        public class Restored(public val profile: AdaptyProfile) : Event()

        /**
         * This event occurs when an error is happened.
         */
        public class Error(public val error: AdaptyError, public val where: Where) : Event() {
            public enum class Where { PURCHASE, RESTORE }
        }
    }
}