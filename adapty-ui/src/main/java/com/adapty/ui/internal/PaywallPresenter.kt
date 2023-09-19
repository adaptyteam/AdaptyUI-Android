@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.app.Activity
import android.os.Handler
import android.os.Looper
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
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyUiPersonalizedOfferResolver
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
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver,
    ) {
        checkViewContext(paywallView)

        val templateConfig = TemplateConfig.from(viewConfig)

        val interactionListener = object : PaywallUiManager.InteractionListener {
            override fun onProductSelected(product: AdaptyPaywallProduct) {
                selectedProduct = product
                log(VERBOSE) { "$LOG_PREFIX $flowKey select product: ${product.vendorProductId}" }
                paywallView.eventListener?.onProductSelected(product, paywallView)
            }

            override fun onPurchaseButtonClicked() {
                selectedProduct?.let { product ->
                    performMakePurchase(paywallView, product, personalizedOfferResolver)
                }
            }

            override fun onRestoreButtonClicked() {
                performRestorePurchases(paywallView)
            }

            override fun onUrlClicked(url: String) {
                paywallView.eventListener?.onActionPerformed(
                    AdaptyUI.Action.OpenUrl(url),
                    paywallView,
                )
            }

            override fun onCustomActionInvoked(customId: String) {
                paywallView.eventListener?.onActionPerformed(
                    AdaptyUI.Action.Custom(customId),
                    paywallView,
                )
            }

            override fun onCloseClicked() {
                paywallView.eventListener?.onActionPerformed(AdaptyUI.Action.Close, paywallView)
            }
        }

        uiManager.buildLayout(
            paywallView,
            templateConfig,
            paywall,
            products,
            insets,
            interactionListener,
        )

        if (products.isNullOrEmpty()) {
            loadProducts(paywall, templateConfig, paywallView, interactionListener)
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

    fun onSizeChanged(w: Int, h: Int) {
        uiManager.onSizeChanged(w, h)
    }

    private fun performMakePurchase(
        paywallView: AdaptyPaywallView,
        product: AdaptyPaywallProduct,
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver,
    ) {
        val activity = paywallView.context as? Activity ?: return
        uiManager.toggleLoadingView(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase begin" }
        paywallView.eventListener?.onPurchaseStarted(product, paywallView)
        val isOfferPersonalized = personalizedOfferResolver.resolve(product)
        Adapty.makePurchase(activity, product, isOfferPersonalized = isOfferPersonalized) { result ->
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
                            paywallView.eventListener?.onPurchaseFailure(
                                result.error,
                                product,
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
                    paywallView.eventListener?.onRestoreSuccess(result.value, paywallView)
                }
                is AdaptyResult.Error -> {
                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey restorePurchases error: ${result.error.message}" }
                    paywallView.eventListener?.onRestoreFailure(result.error, paywallView)
                }
            }
        }
    }

    private fun loadProducts(
        paywall: AdaptyPaywall,
        templateConfig: TemplateConfig,
        paywallView: AdaptyPaywallView,
        interactionListener: PaywallUiManager.InteractionListener,
    ) {
        uiManager.toggleLoadingView(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts begin" }
        Adapty.getPaywallProducts(paywall) { result ->
            when (result) {
                is AdaptyResult.Success -> {
                    uiManager.onProductsLoaded(result.value, templateConfig, interactionListener)
                    uiManager.toggleLoadingView(false)
                    log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts success" }
                }
                is AdaptyResult.Error -> {
                    val shouldRetry =
                        paywallView.eventListener?.onLoadingProductsFailure(result.error, paywallView) ?: false
                    if (shouldRetry) {
                        handler.postDelayed({
                            loadProducts(paywall, templateConfig, paywallView, interactionListener)
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