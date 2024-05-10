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
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyUiPersonalizedOfferResolver
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.adapty.utils.AdaptyResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallPresenter(
    private val flowKey: String,
    private val isObserverMode: Boolean,
    private val uiManager: PaywallUiManager,
) {

    private var selectedProduct: AdaptyPaywallProduct? = null

    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }

    fun showPaywall(
        paywallView: AdaptyPaywallView,
        viewConfig: AdaptyUI.ViewConfiguration,
        products: List<AdaptyPaywallProduct>?,
        insets: AdaptyPaywallInsets,
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver,
        tagResolver: AdaptyUiTagResolver,
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
                    if (!isObserverMode) {
                        if (paywallView.observerModeHandler != null)
                            log(WARN) { "$LOG_PREFIX $flowKey You should not pass observerModeHandler if you're using Adapty in Full Mode" }
                        performMakePurchase(paywallView, product, personalizedOfferResolver)
                    } else {
                        val observerModeHandler = paywallView.observerModeHandler
                        if (observerModeHandler != null) {
                            log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onPurchaseInitiated begin" }
                            observerModeHandler.onPurchaseInitiated(
                                product,
                                viewConfig.paywall,
                                paywallView,
                                {
                                    log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onStartPurchase called" }
                                    uiManager.toggleLoadingView(true)
                                },
                                {
                                    log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onFinishPurchase called" }
                                    uiManager.toggleLoadingView(false)
                                },
                            )
                        } else {
                            log(WARN) { "$LOG_PREFIX $flowKey In order to handle purchases in Observer Mode enabled, provide the observerModeHandler!" }
                            performMakePurchase(paywallView, product, personalizedOfferResolver)
                        }
                    }
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
            viewConfig.paywall,
            products,
            insets,
            tagResolver,
            interactionListener,
        )

        if (products.isNullOrEmpty()) {
            loadProducts(viewConfig.paywall, templateConfig, paywallView, tagResolver, interactionListener)
        }

        log(VERBOSE) { "$LOG_PREFIX $flowKey logShowPaywall begin" }
        Adapty.logShowPaywall(viewConfig.paywall, mapOf("paywall_builder_id" to viewConfig.id)) { error ->
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
        val subscriptionUpdateParams =
            paywallView.eventListener?.onAwaitingSubscriptionUpdateParams(product, paywallView)
        val isOfferPersonalized = personalizedOfferResolver.resolve(product)
        paywallView.eventListener?.onPurchaseStarted(product, paywallView)
        Adapty.makePurchase(activity, product, subscriptionUpdateParams, isOfferPersonalized) { result ->
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
        paywallView.eventListener?.onRestoreStarted(paywallView)
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
        tagResolver: AdaptyUiTagResolver,
        interactionListener: PaywallUiManager.InteractionListener,
    ) {
        uiManager.toggleLoadingView(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts begin" }
        Adapty.getPaywallProducts(paywall) { result ->
            when (result) {
                is AdaptyResult.Success -> {
                    uiManager.onProductsLoaded(result.value, paywall, templateConfig, tagResolver, interactionListener)
                    uiManager.toggleLoadingView(false)
                    log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts success" }
                }
                is AdaptyResult.Error -> {
                    val shouldRetry =
                        paywallView.eventListener?.onLoadingProductsFailure(result.error, paywallView) ?: false
                    if (shouldRetry) {
                        handler.postDelayed({
                            loadProducts(paywall, templateConfig, paywallView, tagResolver, interactionListener)
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