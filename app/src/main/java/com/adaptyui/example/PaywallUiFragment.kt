package com.adaptyui.example

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener

class PaywallUiFragment : Fragment(R.layout.fragment_paywall_ui) {

    companion object {
        fun newInstance(
            paywall: AdaptyPaywall,
            products: List<AdaptyPaywallProduct>,
            viewConfig: AdaptyViewConfiguration,
        ) =
            PaywallUiFragment().apply {
                this.paywall = paywall
                this.products = products
                this.viewConfiguration = viewConfig
            }
    }

    private var viewConfiguration: AdaptyViewConfiguration? = null
    private var paywall: AdaptyPaywall? = null
    private var products = listOf<AdaptyPaywallProduct>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val paywallView = view as? AdaptyPaywallView ?: return
        val viewConfig = viewConfiguration ?: return
        val paywall = paywall ?: return

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
            paywallView.showPaywall(paywall, products, viewConfig, paywallInsets)
        }

        /**
         * Also you can get the `AdaptyPaywallView` and set eventListener and paywall right away
         * by calling `AdaptyUi.getPaywallView()`
         */
    }
}