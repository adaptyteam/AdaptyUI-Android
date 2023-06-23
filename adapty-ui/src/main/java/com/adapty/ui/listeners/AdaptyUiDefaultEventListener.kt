package com.adapty.ui.listeners

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.Event.Error
import com.adapty.ui.AdaptyUI.Event.Restored

public open class AdaptyUiDefaultEventListener : AdaptyUiEventListener {

    public override fun onCloseButtonClick(view: AdaptyPaywallView) {
        (view.context as? Activity)?.onBackPressed()
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

    public override fun onUrlClicked(url: String, view: AdaptyPaywallView) {
        (view.context as? Activity)?.startActivity(
            Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "")
        )
    }

    public override fun showAlert(
        event: AdaptyUI.Event,
        view: AdaptyPaywallView,
    ) {
        when (event) {
            is Restored -> createDialog(
                view.context,
                null,
                "Successfully restored purchases!",
                DialogInterface.OnDismissListener {
                    doAfterAlert(event, view)
                }
            ).show()
            is Error -> createDialog(
                view.context,
                "Error Occured",
                event.error.message.orEmpty(),
                DialogInterface.OnDismissListener {
                    doAfterAlert(event, view)
                }
            ).show()
        }
    }

    public fun doAfterAlert(event: AdaptyUI.Event, view: AdaptyPaywallView) {
        when (event) {
            is Restored -> onRestoreSuccess(event.profile, view)
            is Error.OnPurchase -> onPurchaseFailure(event.error, event.product, view)
            is Error.OnRestore -> onRestoreFailure(event.error, view)
        }
    }

    private fun createDialog(
        context: Context,
        title: String?,
        message: String,
        onDismissListener: DialogInterface.OnDismissListener,
    ) =
        AlertDialog.Builder(context)
            .setOnDismissListener(onDismissListener)
            .apply { title?.let(::setTitle) }
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
}