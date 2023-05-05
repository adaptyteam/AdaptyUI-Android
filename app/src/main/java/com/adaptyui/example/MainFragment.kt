package com.adaptyui.example

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import com.adapty.Adapty
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.utils.AdaptyResult

/**
 * In order to receive full info about the products and open visual paywalls,
 * please change sample's applicationId in app/build.gradle to yours
 */

class MainFragment : Fragment(R.layout.fragment_main) {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(context)
    }

    private fun presentPaywall(
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>,
        viewConfiguration: AdaptyViewConfiguration
    ) {
        val paywallFragment =
            PaywallUiFragment.newInstance(
                paywall,
                products,
                viewConfiguration,
            )

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_up,
                R.anim.slide_down,
                R.anim.slide_up,
                R.anim.slide_down,
            )
            .addToBackStack(null)
            .add(android.R.id.content, paywallFragment)
            .commit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.onReceiveSystemBarsInsets { insets ->
            view.setPadding(
                view.paddingStart,
                view.paddingTop + insets.top,
                view.paddingEnd,
                view.paddingBottom + insets.bottom,
            )
        }

        val errorView = view.findViewById<TextView>(R.id.error_message)
        val paywallIdView = view.findViewById<EditText>(R.id.paywall_id)

        val paywallRelatedViews = view.findViewById<Group>(R.id.paywall_related_views)
        val visualValue = view.findViewById<TextView>(R.id.visual_value)
        val variationValue = view.findViewById<TextView>(R.id.variation_value)
        val revisionValue = view.findViewById<TextView>(R.id.revision_value)
        val localeValue = view.findViewById<TextView>(R.id.locale_value)
        val presentPaywall = view.findViewById<Button>(R.id.present_paywall)

        view.findViewById<View>(R.id.load_button).setOnClickListener {
            progressDialog.show()

            paywallRelatedViews.visibility = View.GONE
            errorView.text = ""

            Adapty.getPaywall(paywallIdView.text.toString()) { paywallResult ->
                when (paywallResult) {
                    is AdaptyResult.Success -> {
                        val paywall = paywallResult.value
                        Adapty.getPaywallProducts(paywall) { productResult ->
                            progressDialog.cancel()

                            when (productResult) {
                                is AdaptyResult.Success -> {
                                    val products = productResult.value

                                    presentPaywall.setOnClickListener {
                                        progressDialog.show()

                                        errorView.text = ""

                                        Adapty.getViewConfiguration(paywall) { configResult ->
                                            progressDialog.cancel()
                                            when (configResult) {
                                                is AdaptyResult.Success -> {
                                                    val viewConfig = configResult.value

                                                    presentPaywall(paywall, products, viewConfig)
                                                }
                                                is AdaptyResult.Error -> {
                                                    errorView.text =
                                                        "error:\n${configResult.error.message}"
                                                }
                                            }
                                        }
                                    }

                                    paywallRelatedViews.visibility = View.VISIBLE
                                    variationValue.text = paywall.variationId
                                    revisionValue.text = "${paywall.revision}"
                                    localeValue.text = paywall.locale

                                    if (paywall.hasViewConfiguration) {
                                        visualValue.text = "yes"
                                        with(presentPaywall) {
                                            isEnabled = true
                                            text = "Present paywall"
                                        }

                                    } else {
                                        visualValue.text = "no"
                                        with(presentPaywall) {
                                            isEnabled = false
                                            text = "No view configuration"
                                        }
                                    }
                                }
                                is AdaptyResult.Error -> {
                                    /**
                                     * If the error code is `AdaptyErrorCode.NO_PRODUCT_IDS_FOUND`, please make sure you have changed your applicationId.
                                     *
                                     * In order to receive products and open visual paywalls,
                                     * please change sample's applicationId in app/build.gradle to yours
                                     */
                                    errorView.text = "error:\n${productResult.error.message}"
                                }
                            }
                        }
                    }
                    is AdaptyResult.Error -> {
                        errorView.text = "error:\n${paywallResult.error.message}"
                        progressDialog.cancel()
                    }
                }
            }
        }
    }
}