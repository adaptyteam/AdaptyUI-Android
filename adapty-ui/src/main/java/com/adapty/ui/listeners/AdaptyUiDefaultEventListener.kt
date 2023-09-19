package com.adapty.ui.listeners

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.LOG_PREFIX_ERROR
import com.adapty.ui.internal.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

public open class AdaptyUiDefaultEventListener : AdaptyUiEventListener {

    override fun onActionPerformed(action: AdaptyUI.Action, view: AdaptyPaywallView) {
        when (action) {
            AdaptyUI.Action.Close -> (view.context as? Activity)?.onBackPressed()
            is AdaptyUI.Action.OpenUrl -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                if (view.context.packageManager.resolveActivity(intent, 0) == null) {
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't find an app that can process this url" }
                } else {
                    view.context.startActivity(Intent.createChooser(intent, ""))
                }
            }
            is AdaptyUI.Action.Custom -> Unit
        }
    }

    public override fun onLoadingProductsFailure(
        error: AdaptyError,
        view: AdaptyPaywallView,
    ): Boolean = false

    override fun onProductSelected(
        product: AdaptyPaywallProduct,
        view: AdaptyPaywallView,
    ) {}

    public override fun onPurchaseCanceled(
        product: AdaptyPaywallProduct,
        view: AdaptyPaywallView,
    ) {}

    public override fun onPurchaseFailure(
        error: AdaptyError,
        product: AdaptyPaywallProduct,
        view: AdaptyPaywallView,
    ) {}

    override fun onPurchaseStarted(
        product: AdaptyPaywallProduct,
        view: AdaptyPaywallView,
    ) {}

    public override fun onPurchaseSuccess(
        profile: AdaptyProfile?,
        product: AdaptyPaywallProduct,
        view: AdaptyPaywallView,
    ) {
        (view.context as? Activity)?.onBackPressed()
    }

    public override fun onRenderingError(
        error: AdaptyError,
        view: AdaptyPaywallView,
    ) {}

    public override fun onRestoreFailure(
        error: AdaptyError,
        view: AdaptyPaywallView,
    ) {}

    public override fun onRestoreSuccess(
        profile: AdaptyProfile,
        view: AdaptyPaywallView,
    ) {}
}