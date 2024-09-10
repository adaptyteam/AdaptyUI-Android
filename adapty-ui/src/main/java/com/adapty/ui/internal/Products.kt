@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.view.View
import android.widget.TextView
import androidx.annotation.RestrictTo
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.PriceFormatter
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPeriodUnit
import com.adapty.models.AdaptyPeriodUnit.DAY
import com.adapty.models.AdaptyPeriodUnit.MONTH
import com.adapty.models.AdaptyPeriodUnit.WEEK
import com.adapty.models.AdaptyPeriodUnit.YEAR
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.ui.AdaptyUI.ViewConfiguration.Component
import java.math.BigDecimal
import java.math.RoundingMode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Products(
    val paywallOrderedProducts: List<ProductInfo>,
    val blockType: BlockType,
    val initiatePurchaseOnTap: Boolean,
) {
    sealed class BlockType {
        object Single: BlockType()

        object Vertical: Multiple()

        object Horizontal: Multiple()

        sealed class Multiple: BlockType()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductInfo(
    val title: Component.Text?,
    private val subtitleDefault: Component.Text?,
    private val subtitlePayUpfront: Component.Text?,
    private val subtitlePayAsYouGo: Component.Text?,
    private val subtitleFreeTrial: Component.Text?,
    val secondTitle: Component.Text?,
    val secondSubtitle: Component.Text?,
    val button: Component.Button?,
    val tagText: Component.Text?,
    val tagShape: Component.Shape?,
    val isMain: Boolean,
) {

    fun getSubtitle(product: AdaptyPaywallProduct): Component.Text? {
        return when(product.firstDiscountOfferOrNull()?.paymentMode) {
            PaymentMode.FREE_TRIAL -> subtitleFreeTrial
            PaymentMode.PAY_AS_YOU_GO -> subtitlePayAsYouGo
            PaymentMode.PAY_UPFRONT -> subtitlePayUpfront
            else -> subtitleDefault
        } ?: subtitleDefault
    }

    val hasSubtitle: Boolean get() = (subtitleDefault ?: subtitlePayUpfront ?: subtitlePayAsYouGo ?: subtitleFreeTrial) != null

    companion object {
        fun from(map: Map<String, Component>, isMainProduct: Boolean): ProductInfo {
            return ProductInfo(
                title = map["title"] as? Component.Text,
                subtitleDefault = map["subtitle"] as? Component.Text,
                subtitlePayUpfront = map["subtitle_payupfront"] as? Component.Text,
                subtitlePayAsYouGo = map["subtitle_payasyougo"] as? Component.Text,
                subtitleFreeTrial = map["subtitle_freetrial"] as? Component.Text,
                secondTitle = map["second_title"] as? Component.Text,
                secondSubtitle = map["second_subtitle"] as? Component.Text,
                button = map["button"] as? Component.Button,
                tagText = (map["tag_text"] as? Component.Text),
                tagShape = (map["tag_shape"] as? Component.Shape),
                isMain = isMainProduct,
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductViewsBundle(
    val productCell: View?,
    val productTitle: TextView?,
    val productSubtitle: TextView?,
    val productSecondTitle: TextView?,
    val productSecondSubtitle: TextView?,
    val productTag: TextView?,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class ProductPlaceholderContentData(
    val placeholder: String,
) {
    class Simple(placeholder: String, val value: String): ProductPlaceholderContentData(placeholder)

    class Extended(placeholder: String, val value: String, product: AdaptyPaywallProduct): ProductPlaceholderContentData(placeholder) {
        val currencyCode = product.price.currencyCode
        val currencySymbol = product.price.currencySymbol
    }

    class Drop(placeholder: String): ProductPlaceholderContentData(placeholder)

    object Tags {
        const val title = "</TITLE/>"
        const val price = "</PRICE/>"
        const val pricePerDay = "</PRICE_PER_DAY/>"
        const val pricePerWeek = "</PRICE_PER_WEEK/>"
        const val pricePerMonth = "</PRICE_PER_MONTH/>"
        const val pricePerYear = "</PRICE_PER_YEAR/>"
        const val offerPrice = "</OFFER_PRICE/>"
        const val offerPeriod = "</OFFER_PERIOD/>"
        const val offerNumberOfPeriods = "</OFFER_NUMBER_OF_PERIOD/>"

        val all = setOf(
            title,
            price,
            pricePerDay,
            pricePerWeek,
            pricePerMonth,
            pricePerYear,
            offerPrice,
            offerPeriod,
            offerNumberOfPeriods,
        )
    }

    companion object {

        fun create(product: AdaptyPaywallProduct, priceFormatter: PriceFormatter): List<ProductPlaceholderContentData> {
            val firstDiscountOfferIfExists = product.firstDiscountOfferOrNull()

            return listOf(
                from(Tags.title, product.localizedTitle),
                from(Tags.price, product.price.localizedString, product),
                from(Tags.pricePerDay, createPricePerPeriodText(product, DAY, priceFormatter), product),
                from(Tags.pricePerWeek, createPricePerPeriodText(product, WEEK, priceFormatter), product),
                from(Tags.pricePerMonth, createPricePerPeriodText(product, MONTH, priceFormatter), product),
                from(Tags.pricePerYear, createPricePerPeriodText(product, YEAR, priceFormatter), product),
                from(Tags.offerPrice, firstDiscountOfferIfExists?.price?.localizedString, product),
                from(Tags.offerPeriod, firstDiscountOfferIfExists?.localizedSubscriptionPeriod),
                from(Tags.offerNumberOfPeriods, firstDiscountOfferIfExists?.localizedNumberOfPeriods),
            )
        }

        private fun from(placeholder: String, value: String?, product: AdaptyPaywallProduct? = null): ProductPlaceholderContentData =
            when {
                value == null -> Drop(placeholder)
                product == null -> Simple(placeholder, value)
                else -> Extended(placeholder, value, product)
            }

        private fun createPricePerPeriodText(product: AdaptyPaywallProduct, targetUnit: AdaptyPeriodUnit, priceFormatter: PriceFormatter): String? {
            val subscriptionPeriod = product.subscriptionDetails?.subscriptionPeriod
            val price = product.price
            val unit =
                subscriptionPeriod?.unit?.takeIf { it in listOf(WEEK, YEAR, MONTH) } ?: return null
            val numberOfUnits = subscriptionPeriod.numberOfUnits.takeIf { it > 0 } ?: return null
            val localizedPrice = price.localizedString

            return when {
                unit == targetUnit && numberOfUnits == 1 -> localizedPrice
                else -> {
                    val pricePerPeriod = when (targetUnit) {
                        unit -> price.amount.divide(
                            numberOfUnits.toBigDecimal(),
                            4,
                            RoundingMode.CEILING
                        )
                        DAY -> toDaily(price, unit, numberOfUnits)
                        WEEK -> toWeekly(price, unit, numberOfUnits)
                        MONTH -> toMonthly(price, unit, numberOfUnits)
                        else -> toYearly(price, unit, numberOfUnits)
                    }

                    priceFormatter.format(pricePerPeriod, localizedPrice)
                }
            }
        }

        private fun toYearly(
            price: AdaptyPaywallProduct.Price,
            unit: AdaptyPeriodUnit,
            numberOfUnits: Int,
        ): BigDecimal {
            val unitsInYear = when (unit) {
                YEAR -> 1
                MONTH -> 12
                else -> 52
            }
            val divisor = numberOfUnits.toBigDecimal()
            val multiplier = unitsInYear.toBigDecimal()
            return price.amount.divide(divisor, 4, RoundingMode.CEILING) * multiplier
        }

        private fun toMonthly(
            price: AdaptyPaywallProduct.Price,
            unit: AdaptyPeriodUnit,
            numberOfUnits: Int,
        ): BigDecimal {
            val divisor: BigDecimal
            val multiplier: BigDecimal
            when (unit) {
                YEAR -> {
                    divisor = (12 * numberOfUnits).toBigDecimal()
                    multiplier = BigDecimal.ONE
                }
                MONTH -> {
                    divisor = numberOfUnits.toBigDecimal()
                    multiplier = BigDecimal.ONE
                }
                else -> {
                    divisor = numberOfUnits.toBigDecimal()
                    multiplier = 4.toBigDecimal()
                }
            }
            return price.amount.divide(divisor, 4, RoundingMode.CEILING) * multiplier
        }

        private fun toWeekly(
            price: AdaptyPaywallProduct.Price,
            unit: AdaptyPeriodUnit,
            numberOfUnits: Int,
        ): BigDecimal {
            val weeksInUnit = when (unit) {
                YEAR -> 52
                MONTH -> 4
                else -> 1
            }
            val divisor = (weeksInUnit * numberOfUnits).toBigDecimal()
            return price.amount.divide(divisor, 4, RoundingMode.CEILING)
        }

        private fun toDaily(
            price: AdaptyPaywallProduct.Price,
            unit: AdaptyPeriodUnit,
            numberOfUnits: Int,
        ): BigDecimal {
            val daysInUnit = when (unit) {
                YEAR -> 365
                MONTH -> 30
                else -> 7
            }
            val divisor = (daysInUnit * numberOfUnits).toBigDecimal()
            return price.amount.divide(divisor, 4, RoundingMode.CEILING)
        }
    }
}