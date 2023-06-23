@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.RestrictTo
import com.adapty.Adapty
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI.Event.Error
import com.adapty.ui.AdaptyUI.Event.Restored
import com.adapty.ui.listeners.AdaptyUiProductTitleResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallPresenter(
    private val flowKey: String,
    private val uiManager: PaywallUiManager,
) {

    private var selectedProduct: AdaptyPaywallProduct? = null

    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }

    fun showPaywall(
        paywallView: AdaptyPaywallView,
        viewConfig: AdaptyViewConfiguration,
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        insets: AdaptyPaywallInsets,
        productTitleResolver: AdaptyUiProductTitleResolver,
    ) {
        checkViewContext(paywallView)

        val interactionListener = object : PaywallUiManager.InteractionListener {
            override fun onProductSelected(product: AdaptyPaywallProduct) {
                selectedProduct = product
                log(VERBOSE) { "$LOG_PREFIX $flowKey select product: ${product.vendorProductId}" }
                paywallView.eventListener?.onProductSelected(product, paywallView)
            }

            override fun onPurchaseButtonClicked() {
                selectedProduct?.let { product ->
                    performMakePurchase(paywallView, product)
                }
            }

            override fun onRestoreButtonClicked() {
                performRestorePurchases(paywallView)
            }

            override fun onUrlClicked(url: String) {
                paywallView.eventListener?.onUrlClicked(url, paywallView)
            }

            override fun onCloseClicked() {
                paywallView.eventListener?.onCloseButtonClick(paywallView)
            }
        }

        uiManager.buildLayout(
            paywallView,
            viewConfig,
            paywall,
            products,
            insets,
            interactionListener,
            productTitleResolver,
        )

        if (products.isNullOrEmpty()) {
            loadProducts(paywall, viewConfig, paywallView, interactionListener, productTitleResolver)
        }

        log(VERBOSE) { "$LOG_PREFIX $flowKey logShowPaywall begin" }
        Adapty.logShowPaywall(paywall, viewConfig) { error ->
            if (error != null) {
                log(ERROR) { "$LOG_PREFIX_ERROR $flowKey logShowPaywall error: ${error.message}" }
            } else {
                log(VERBOSE) { "$LOG_PREFIX $flowKey logShowPaywall success" }
            }
        }
    }

    fun clearOldPaywall() {
        handler.removeCallbacksAndMessages(null)
        uiManager.clearOldPaywall()
        selectedProduct = null
    }

    fun onSizeChanged(view: View, w: Int, h: Int) {
        uiManager.onSizeChanged(view, w, h)
    }

    private fun performMakePurchase(
        paywallView: AdaptyPaywallView,
        product: AdaptyPaywallProduct,
    ) {
        val activity = paywallView.context as? Activity ?: return
        uiManager.toggleLoadingView(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase begin" }
        paywallView.eventListener?.onPurchaseStarted(product, paywallView)
        Adapty.makePurchase(activity, product) { result ->
            uiManager.toggleLoadingView(false)
            when (result) {
                is AdaptyResult.Success -> {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase success" }
                    paywallView.eventListener?.onPurchaseSuccess(
                        result.value,
                        product,
                        paywallView,
                    )
                }
                is AdaptyResult.Error -> {
                    val error = result.error
                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey makePurchase error: ${error.message}" }
                    when (error.adaptyErrorCode) {
                        AdaptyErrorCode.USER_CANCELED -> {
                            paywallView.eventListener?.onPurchaseCanceled(product, paywallView)
                        }
                        else -> {
                            paywallView.eventListener?.showAlert(
                                Error.OnPurchase(result.error, product),
                                paywallView,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun performRestorePurchases(paywallView: AdaptyPaywallView) {
        uiManager.toggleLoadingView(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases begin" }
        Adapty.restorePurchases { result ->
            uiManager.toggleLoadingView(false)
            when (result) {
                is AdaptyResult.Success -> {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases success" }
                    paywallView.eventListener?.showAlert(
                        Restored(result.value),
                        paywallView,
                    )
                }
                is AdaptyResult.Error -> {
                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey restorePurchases error: ${result.error.message}" }
                    paywallView.eventListener?.showAlert(
                        Error.OnRestore(result.error),
                        paywallView,
                    )
                }
            }
        }
    }

    private fun loadProducts(
        paywall: AdaptyPaywall,
        viewConfig: AdaptyViewConfiguration,
        paywallView: AdaptyPaywallView,
        interactionListener: PaywallUiManager.InteractionListener,
        productTitleResolver: AdaptyUiProductTitleResolver,
    ) {
        uiManager.toggleLoadingView(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts begin" }
        Adapty.getPaywallProducts(paywall) { result ->
            when (result) {
                is AdaptyResult.Success -> {
                    uiManager.onProductsLoaded(result.value, viewConfig, interactionListener, productTitleResolver)
                    uiManager.toggleLoadingView(false)
                    log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts success" }
                }
                is AdaptyResult.Error -> {
                    val shouldRetry =
                        paywallView.eventListener?.onLoadingProductsFailure(result.error, paywallView) ?: false
                    if (shouldRetry) {
                        handler.postDelayed({
                            loadProducts(paywall, viewConfig, paywallView, interactionListener, productTitleResolver)
                        }, LOADING_PRODUCTS_RETRY_DELAY)
                    } else {
                        uiManager.toggleLoadingView(false)
                    }
                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey loadProducts error: ${result.error.message}" }
                }
            }
        }
    }

    private fun checkViewContext(paywallView: AdaptyPaywallView) {
        if (paywallView.context !is Activity) throw adaptyError(
            null,
            "AdaptyUIError: please provide activity context!",
            AdaptyErrorCode.WRONG_PARAMETER,
        )
    }
}