package com.adapty.ui.internal

import android.graphics.Paint
import com.adapty.models.AdaptyEligibility.ELIGIBLE
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPeriodUnit.*
import java.math.RoundingMode

internal fun AdaptyPaywallProduct.hasDiscount(): Boolean =
    introductoryOfferEligibility == ELIGIBLE && (introductoryDiscount != null || freeTrialPeriod != null)

internal fun AdaptyPaywallProduct.createDiscountText(paint: Paint): String? {
    when {
        introductoryOfferEligibility != ELIGIBLE -> return null
        localizedFreeTrialPeriod != null -> return "$localizedFreeTrialPeriod $PRODUCT_DISCOUNT_SUFFIX_FREE_TRIAL"
        else -> {
            val introDiscount = introductoryDiscount ?: return null
            val introPrice = createTextFromLocalizedPrice(introDiscount.localizedPrice, paint)
            return when {
                introDiscount.numberOfPeriods > 1 -> "$introPrice $PRODUCT_DISCOUNT_INTERFIX_FOR_THE_FIRST ${introDiscount.localizedNumberOfPeriods}"
                else -> "$introPrice $PRODUCT_DISCOUNT_INTERFIX_FOR ${introDiscount.localizedSubscriptionPeriod}"
            }
        }
    }
}

internal fun AdaptyPaywallProduct.canConvertPriceToWeekly(): Boolean =
    subscriptionPeriod?.unit in listOf(YEAR, MONTH, WEEK)

internal fun AdaptyPaywallProduct.createPricePerWeekText(paint: Paint): String? {
    val unit = subscriptionPeriod?.unit?.takeIf { it in listOf(WEEK, YEAR, MONTH) } ?: return null
    val numberOfUnits = subscriptionPeriod?.numberOfUnits?.takeIf { it > 0 } ?: return null

    return when {
        unit == WEEK && numberOfUnits == 1 -> localizedPrice
        else -> {
            val pricePerWeek = when (unit) {
                WEEK -> price.divide(numberOfUnits.toBigDecimal(), 4, RoundingMode.CEILING)
                else -> {
                    val divisor = (when (unit) {
                        YEAR -> 365
                        else -> 30
                    } * numberOfUnits).toBigDecimal()

                    price.divide(divisor, 4, RoundingMode.CEILING) * 7.toBigDecimal()
                }
            }

            val pricePerWeekString =
                pricePerWeek
                    .setScale(2, RoundingMode.CEILING)
                    .toPlainString()
            var startIndex = -1
            var endIndex = -1
            for ((i, ch) in localizedPrice.withIndex()) {
                if (ch.isDigit()) {
                    if (startIndex == -1) startIndex = i
                    endIndex = i
                }
            }
            if (startIndex > -1 && endIndex in startIndex until localizedPrice.length) {
                localizedPrice.replace(
                    localizedPrice.substring(startIndex..endIndex),
                    pricePerWeekString
                )
            } else {
                pricePerWeekString
            }
        }
    }?.let { firstPart ->
        val secondPart = PRODUCT_PRICE_PER_WEEK_SUFFIX

        "${createTextFromLocalizedPrice(firstPart, paint)}/$secondPart"
    }
}

internal fun AdaptyPaywallProduct.createTextFromLocalizedPrice(
    localizedPrice: String,
    paint: Paint,
): String {
    return when {
        localizedPrice.contains(currencyCode, true)
                && currencyCode.isNotBlank()
                && !currencyCode.equals(currencySymbol, true)
                && paint.hasGlyphCompat(currencySymbol) -> {
            localizedPrice.replace(currencyCode, currencySymbol, true)
        }
        else -> localizedPrice
    }
}

private const val PRODUCT_PRICE_PER_WEEK_SUFFIX = "week"
private const val PRODUCT_DISCOUNT_SUFFIX_FREE_TRIAL = "free trial"
private const val PRODUCT_DISCOUNT_INTERFIX_FOR_THE_FIRST = "for the first"
private const val PRODUCT_DISCOUNT_INTERFIX_FOR = "for"