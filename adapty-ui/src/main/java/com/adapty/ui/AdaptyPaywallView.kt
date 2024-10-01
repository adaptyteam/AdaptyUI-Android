package com.adapty.ui

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.internal.ui.AdaptyPaywallInternal
import com.adapty.ui.internal.ui.PaywallViewModel
import com.adapty.ui.internal.ui.PaywallViewModelArgs
import com.adapty.ui.internal.ui.PaywallViewModelFactory
import com.adapty.ui.internal.ui.UserArgs
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.getCurrentLocale
import com.adapty.ui.internal.utils.log
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiPersonalizedOfferResolver
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import java.util.UUID

public class AdaptyPaywallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private val viewModel: PaywallViewModel? by lazy {
        val viewModelStoreOwner = findViewTreeViewModelStoreOwner()
            ?: run {
                log(ERROR) { "$LOG_PREFIX AdaptyPaywallView (${hashCode()}) rendering error: No ViewModelStoreOwner found" }
                return@lazy null
            }
        PaywallViewModelArgs.create(
            "${UUID.randomUUID().toString().hashCode()}",
            null,
            context.getCurrentLocale(),
        )?.let { args ->
            val factory = PaywallViewModelFactory(args)
            ViewModelProvider(viewModelStoreOwner, factory)[PaywallViewModel::class.java]
        }
    }

    /**
     * Should be called only on UI thread
     *
     * If the [AdaptyPaywallView] has been created by calling [AdaptyUI.getPaywallView],
     * calling this method is unnecessary.
     *
     * @param[viewConfiguration] An [AdaptyUI.LocalizedViewConfiguration] object containing information
     * about the visual part of the paywall. To load it, use the [AdaptyUI.getViewConfiguration] method.
     *
     * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
     * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
     * automatically fetch the required products.
     *
     * @param[eventListener] An object that implements the [AdaptyUiEventListener] interface.
     * Use it to respond to different events happening inside the purchase screen.
     * Also you can extend [AdaptyUiDefaultEventListener] so you don't need to override all the methods.
     *
     * @param[personalizedOfferResolver] In case you want to indicate whether the price is personalized ([read more](https://developer.android.com/google/play/billing/integrate#personalized-price)),
     * you can implement [AdaptyUiPersonalizedOfferResolver] and pass your own logic
     * that maps [AdaptyPaywallProduct] to `true`, if the price of the product is personalized, otherwise `false`.
     *
     * @param[tagResolver] If you are going to use custom tags functionality, pass the resolver function here.
     *
     * @param[timerResolver] If you are going to use custom timer functionality, pass the resolver function here.
     *
     * @param[observerModeHandler] If you use Adapty in [Observer mode](https://adapty.io/docs/observer-vs-full-mode),
     * pass the [AdaptyUiObserverModeHandler] implementation to handle purchases on your own.
     */
    @UiThread
    public fun showPaywall(
        viewConfiguration: AdaptyUI.LocalizedViewConfiguration,
        products: List<AdaptyPaywallProduct>?,
        eventListener: AdaptyUiEventListener,
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver = AdaptyUiPersonalizedOfferResolver.DEFAULT,
        tagResolver: AdaptyUiTagResolver = AdaptyUiTagResolver.DEFAULT,
        timerResolver: AdaptyUiTimerResolver = AdaptyUiTimerResolver.DEFAULT,
        observerModeHandler: AdaptyUiObserverModeHandler? = null,
    ) {
        viewModel?.setNewData(
            UserArgs.create(
                viewConfiguration,
                eventListener,
                personalizedOfferResolver,
                tagResolver,
                timerResolver,
                observerModeHandler,
                products,
                ProductLoadingFailureCallback { error -> eventListener.onLoadingProductsFailure(error, context) },
            )
        )
    }

    @Composable
    override fun Content() {
        val viewModel = viewModel ?: return
        if (viewModel.dataState.value != null) {
            AdaptyPaywallInternal(viewModel)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log(VERBOSE) { "$LOG_PREFIX AdaptyPaywallView (${hashCode()}) onAttachedToWindow" }
    }

    override fun onDetachedFromWindow() {
        log(VERBOSE) { "$LOG_PREFIX AdaptyPaywallView (${hashCode()}) onDetachedFromWindow" }
        super.onDetachedFromWindow()
    }
}