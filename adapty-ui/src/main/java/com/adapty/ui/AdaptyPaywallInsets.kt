package com.adapty.ui

/**
 * @property[top] Additional top margin for *close* icon. Useful when the status bar overlaps the [AdaptyPaywallView]
 * @property[bottom] Additional bottom margin for the content. Useful when the navigation bar overlaps the [AdaptyPaywallView]
 */
public class AdaptyPaywallInsets private constructor(
    public val top: Int,
    public val bottom: Int,
) {
    public companion object {
        /**
         * @param[top] Additional top margin for *close* icon. Useful when the status bar overlaps the [AdaptyPaywallView]
         * @param[bottom] Additional bottom margin for the content. Useful when the navigation bar overlaps the [AdaptyPaywallView]
         */
        @JvmStatic
        public fun of(top: Int, bottom: Int): AdaptyPaywallInsets = AdaptyPaywallInsets(top, bottom)

        /**
         * You can use this field when none of the system bars overlap the [AdaptyPaywallView]
         */
        @JvmField
        public val NONE: AdaptyPaywallInsets = of(top = 0, bottom = 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdaptyPaywallInsets) return false

        if (top != other.top) return false
        if (bottom != other.bottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = top
        result = 31 * result + bottom
        return result
    }

    override fun toString(): String {
        return "AdaptyPaywallInsets(top=$top, bottom=$bottom)"
    }
}