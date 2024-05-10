package com.adapty.ui.internal

import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintSet
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI.ViewConfiguration.Component
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallUiManager(
    private val flowKey: String,
    private val viewHelper: ViewHelper,
    private val layoutHelper: LayoutHelper,
    private val productBlockRenderer: ProductBlockRenderer,
) {

    private var paywallScreen: PaywallScreen? = null

    fun onSizeChanged(w: Int, h: Int) {
        paywallScreen?.onSizeChanged(w, h)
    }

    fun buildLayout(
        paywallView: AdaptyPaywallView,
        templateConfig: TemplateConfig,
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        insets: AdaptyPaywallInsets,
        tagResolver: AdaptyUiTagResolver,
        interactionListener: InteractionListener,
    ) {
        val renderer = PaywallRenderer.create(templateConfig, productBlockRenderer, viewHelper, layoutHelper)
        val actionListener: (Component.Button.Action) -> Unit = { action ->
            when (action) {
                is Component.Button.Action.OpenUrl -> {
                    templateConfig.getString(action.urlId, tagResolver)?.let { url ->
                        log(VERBOSE) { "$LOG_PREFIX $flowKey action: open url" }
                        interactionListener.onUrlClicked(url)
                    }
                }

                is Component.Button.Action.Close -> {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey action: close" }
                    interactionListener.onCloseClicked()
                }

                is Component.Button.Action.Restore -> {
                    interactionListener.onRestoreButtonClicked()
                }

                is Component.Button.Action.Custom -> {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey action: custom" }
                    interactionListener.onCustomActionInvoked(action.customId)
                }
            }
        }

        paywallScreen = renderer.render(
            paywall,
            products,
            paywallView,
            insets,
            tagResolver,
            actionListener,
            interactionListener,
        )
    }

    fun onProductsLoaded(
        products: List<AdaptyPaywallProduct>,
        paywall: AdaptyPaywall,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        interactionListener: InteractionListener,
    ) {
        val paywallScreen = paywallScreen ?: return

        val contentContainer = paywallScreen.contentContainer
        val purchaseButton = paywallScreen.purchaseButton
        val constraintSet = ConstraintSet().apply { clone(contentContainer) }

        val productBlock = templateConfig.getProductBlock(paywall)

        productBlockRenderer.fillInnerProductTexts(
            products,
            paywallScreen,
            productBlock,
            purchaseButton.textView,
            templateConfig,
            tagResolver,
            interactionListener,
        )

        constraintSet.applyTo(contentContainer)
    }

    fun clearOldPaywall() {
        paywallScreen = null
    }

    fun toggleLoadingView(show: Boolean) {
        paywallScreen?.toggleLoadingView(show)
    }

    interface InteractionListener {
        fun onProductSelected(product: AdaptyPaywallProduct)
        fun onPurchaseButtonClicked()
        fun onRestoreButtonClicked()
        fun onUrlClicked(url: String)
        fun onCustomActionInvoked(customId: String)
        fun onCloseClicked()
    }
}