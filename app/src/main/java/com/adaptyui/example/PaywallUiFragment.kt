package com.adaptyui.example

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener

class PaywallUiFragment : Fragment(R.layout.fragment_paywall_ui) {

    companion object {
        fun newInstance(
            viewConfig: AdaptyUI.ViewConfiguration,
            products: List<AdaptyPaywallProduct>,
        ) =
            PaywallUiFragment().apply {
                this.products = products
                this.viewConfiguration = viewConfig
            }
    }

    private var viewConfiguration: AdaptyUI.ViewConfiguration? = null
    private var products = listOf<AdaptyPaywallProduct>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val paywallView = view as? AdaptyPaywallView ?: return
        val viewConfig = viewConfiguration ?: return

        paywallView.setEventListener(
            object: AdaptyUiDefaultEventListener() {

                /**
                 * You can override more methods if needed
                 */

                override fun onRestoreSuccess(
                    profile: AdaptyProfile,
                    view: AdaptyPaywallView,
                ) {
                    if (profile.accessLevels["premium"]?.isActive == true) {
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        )

        /**
         * You need the `onReceiveSystemBarsInsets` callback only in case the status bar or
         * navigation bar overlap the view otherwise it may not be called, so simply
         * call `paywallView.showPaywall(paywall, products, viewConfig, AdaptyPaywallInsets.NONE)`
         */
        paywallView.onReceiveSystemBarsInsets { insets ->
            val paywallInsets = AdaptyPaywallInsets.of(insets.top, insets.bottom)
            val customTags = mapOf("USERNAME" to "Bruce", "CITY" to "Philadelphia")
            paywallView.showPaywall(
                viewConfig,
                products,
                paywallInsets,
                tagResolver = { tag -> customTags[tag] }
            )
        }

        /**
         * Also you can get the `AdaptyPaywallView` and set eventListener and paywall right away
         * by calling `AdaptyUi.getPaywallView()`
         */
    }
}