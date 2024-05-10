package com.adapty.ui.internal

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class RetryLazyVal<out T>(val initializer: () -> T?) : ReadOnlyProperty<Any?, T?> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return value ?: initializer()?.also { value = it }
    }
}

internal fun <T : Any> retryLazy(initializer: () -> T?): ReadOnlyProperty<Any?, T?> = RetryLazyVal(initializer)