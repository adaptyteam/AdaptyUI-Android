package com.adapty.ui.listeners

import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyPaywallView

public fun interface AdaptyUiObserverModeHandler {

    public fun onPurchaseInitiated(
        product: AdaptyPaywallProduct,
        paywall: AdaptyPaywall,
        view: AdaptyPaywallView,
        onStartPurchase: PurchaseStartCallback,
        onFinishPurchase: PurchaseFinishCallback,
    )

    public fun interface PurchaseStartCallback {
        public operator fun invoke()
    }

    public fun interface PurchaseFinishCallback {
        public operator fun invoke()
    }
}