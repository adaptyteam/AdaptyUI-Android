package com.adapty.ui.listeners

import com.adapty.models.AdaptyPaywallProduct

/**
 * Implement this interface to override display names of the products.
 */
public fun interface AdaptyUiProductTitleResolver {
    /**
     * Function that maps a product to a display name on the paywall screen.
     *
     * @param[product] An [AdaptyPaywallProduct] to be displayed.
     *
     * @return a display name for the [product].
     */
    public fun resolve(product: AdaptyPaywallProduct): String

    public companion object {
        /**
         * The default implementation that returns [localizedTitle][AdaptyPaywallProduct.localizedTitle] of the product.
         */
        public val DEFAULT: AdaptyUiProductTitleResolver =
            AdaptyUiProductTitleResolver { product -> product.localizedTitle }
    }
}