package com.adapty.ui.listeners

import android.app.Activity
import android.app.AlertDialog
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI

/**
 * Implement this interface to respond to different events happening inside the purchase screen.
 */
public interface AdaptyUiEventListener {

    /**
     * If the user presses the *close* icon, this callback is invoked.
     *
     * The [default][AdaptyUiDefaultEventListener.onCloseButtonClick] implementation is simply
     * imitating pressing the system back button
     *
     * Note: this callback is *not* invoked when user presses the system back button
     * instead of the *close* icon on the screen.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onCloseButtonClick(view: AdaptyPaywallView)

    /**
     * This callback is invoked in case of errors during the products loading process.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     *
     * @return `true`, if you want to retry products fetching.
     * The [default][AdaptyUiDefaultEventListener.onLoadingProductsFailure] implementation returns `false`.
     */
    public fun onLoadingProductsFailure(
        error: AdaptyError,
        view: AdaptyPaywallView,
    ): Boolean

    /**
     * This callback is invoked when user cancels the purchase manually.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onPurchaseCanceled(view: AdaptyPaywallView)

    /**
     * This callback is invoked when the purchase process fails.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onPurchaseFailure(
        error: AdaptyError,
        view: AdaptyPaywallView,
    )

    /**
     * This callback is invoked when a successful purchase is made.
     *
     * The [default][AdaptyUiDefaultEventListener.onPurchaseSuccess] implementation is simply
     * calling [onBackPressed][Activity.onBackPressed] method of
     * the [Activity] the [AdaptyPaywallView] is attached to.
     *
     * @param[profile] An [AdaptyProfile] object containing up to date information about the user.
     *
     * @param[product] An [AdaptyPaywallProduct] that has been purchased.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onPurchaseSuccess(
        profile: AdaptyProfile?,
        product: AdaptyPaywallProduct,
        view: AdaptyPaywallView,
    )

    /**
     * This callback is invoked in case of errors during the screen rendering process.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onRenderingError(
        error: AdaptyError,
        view: AdaptyPaywallView,
    )

    /**
     * This callback is invoked when the restore process fails.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onRestoreFailure(
        error: AdaptyError,
        view: AdaptyPaywallView,
    )

    /**
     * This callback is invoked when a successful restore is made.
     *
     * Check if the [AdaptyProfile] object contains the desired access level, and if so,
     * you can navigate back from the paywall.
     *
     * @param[profile] An [AdaptyProfile] object containing up to date information about the user.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onRestoreSuccess(
        profile: AdaptyProfile,
        view: AdaptyPaywallView,
    )

    /**
     * If the user presses the "Terms" or "Privacy Policy" buttons, this callback is invoked.
     *
     * The [default][AdaptyUiDefaultEventListener.onUrlClicked] implementation shows a chooser
     * with apps that can open the link.
     *
     * @param[url] A link to the desired page.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun onUrlClicked(url: String, view: AdaptyPaywallView)

    /**
     * In some cases it is necessary to show the message to the user.
     * By overriding this method, you can show the event or error in any way you like.
     *
     * By [default][AdaptyUiDefaultEventListener.showAlert], errors are shown inside
     * the [AlertDialog].
     *
     * Note: the callbacks that follow the dialog in the default implementation (i.e. [onRestoreSuccess])
     * are invoked only after the dialog is dismissed.
     * So if you override [showAlert][AdaptyUiDefaultEventListener.showAlert]
     * from [AdaptyUiDefaultEventListener] without calling `super`, please make sure
     * to call [doAfterAlert][AdaptyUiDefaultEventListener.doAfterAlert] when the said callbacks
     * need to be triggered (or call them manually if you implement [AdaptyUiEventListener] listener directly).
     *
     * @param[event] An [AdaptyUI.Event] value that specifies the reason why the message should be shown to the user.
     *
     * @param[view] An [AdaptyPaywallView] within which the event occurred.
     */
    public fun showAlert(
        event: AdaptyUI.Event,
        view: AdaptyPaywallView,
    )
}