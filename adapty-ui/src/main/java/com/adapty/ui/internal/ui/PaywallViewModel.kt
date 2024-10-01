@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adapty.Adapty
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.di.DIObject
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.OBSERVER_MODE
import com.adapty.internal.utils.CacheRepositoryProxy
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.PriceFormatter
import com.adapty.internal.utils.getOrderedOriginalProductIdMappings
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset.Image
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.text.PriceConverter
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.TagResolver
import com.adapty.ui.internal.text.TextResolver
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.LOADING_PRODUCTS_RETRY_DELAY
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.log
import com.adapty.ui.listeners.AdaptyUiEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiPersonalizedOfferResolver
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.adapty.utils.AdaptyResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale

internal class PaywallViewModel(
    private val flowKey: String,
    private val isObserverMode: Boolean,
    private val mediaFetchService: MediaFetchService,
    private val cacheRepository: CacheRepositoryProxy,
    private val textResolver: TextResolver,
    args: UserArgs?,
) : ViewModel() {

    val dataState = mutableStateOf(args)

    val state = mutableStateMapOf<String, Any>()

    val products = mutableStateMapOf<String, AdaptyPaywallProduct>()

    val assets = mutableStateMapOf<String, Asset>()

    val texts = mutableStateMapOf<String, AdaptyUI.LocalizedViewConfiguration.TextItem>()

    val isLoading = mutableStateOf(false)

    init {
        viewModelScope.launch {
            snapshotFlow { dataState.value }
                .collect { newData ->
                    if (newData == null) return@collect
                    val viewConfig = newData.viewConfig
                    val initialProducts = newData.products
                    updateData(newData)

                    if (initialProducts.isEmpty()) {
                        viewModelScope.launch {
                            toggleLoading(true)
                            val productResult = loadProducts(
                                viewConfig.paywall,
                                newData.productLoadingFailureCallback,
                            )
                            if (productResult is AdaptyResult.Success)
                                products.putAll(associateProductsToIds(productResult.value, viewConfig.paywall))
                            toggleLoading(false)
                        }
                    }
                    viewConfig.assets.forEach { (id, asset) ->
                        if (asset is Asset.RemoteImage) {
                            viewModelScope.launch {
                                val image = loadImage(asset)
                                assets[id] = image
                            }
                        }
                    }
                }
        }
    }

    fun setNewData(newData: UserArgs) {
        dataState.value = newData
        textResolver.setCustomTagResolver(newData.tagResolver)
    }

    private fun updateData(newData: UserArgs) {
        updateState(newData.viewConfig)
        updateAssets(newData.viewConfig)
        updateProducts(newData.products, newData.viewConfig)
        updateTexts(newData.viewConfig)
    }

    private fun updateState(viewConfig: AdaptyUI.LocalizedViewConfiguration) {
        with(state) {
            clear()
            putAll(viewConfig.screens.initialState)
        }
    }

    private fun updateProducts(
        initialProducts: List<AdaptyPaywallProduct>,
        viewConfig: AdaptyUI.LocalizedViewConfiguration,
    ) {
        with(products) {
            clear()
            putAll(associateProductsToIds(initialProducts, viewConfig.paywall))
        }
    }

    private fun updateTexts(
        viewConfig: AdaptyUI.LocalizedViewConfiguration,
    ) {
        with(texts) {
            clear()
            putAll(viewConfig.texts)
        }
    }

    private fun updateAssets(viewConfig: AdaptyUI.LocalizedViewConfiguration) {
        with(assets) {
            clear()
            putAll(
                viewConfig.assets.toList()
                    .mapNotNull { record ->
                        val (id, asset) = record
                        if (asset is Asset.RemoteImage)
                            asset.preview?.let { id to it }
                        else
                            record
                    }
                    .toMap()
            )
        }
    }

    private fun associateProductsToIds(
        products: List<AdaptyPaywallProduct>,
        paywall: AdaptyPaywall,
    ): Map<String, AdaptyPaywallProduct> {
        return if (products.isNotEmpty())
            getOrderedOriginalProductIdMappings(paywall)
                .mapNotNull { (id, vendorProductId) ->
                    products.firstOrNull { it.vendorProductId == vendorProductId }?.let { product ->
                        id to product
                    }
                }
                .toMap()
        else {
            mapOf()
        }
    }

    private suspend fun loadImage(remoteImage: Asset.RemoteImage): Image =
        suspendCancellableCoroutine { continuation ->
            mediaFetchService.loadImage(
                remoteImage,
                handlePreview = null,
                handleResult = { image ->
                    continuation.resumeWith(Result.success(image))
                }
            )
        }

    fun onPurchaseInitiated(
        activity: Activity,
        paywall: AdaptyPaywall,
        product: AdaptyPaywallProduct,
        eventListener: EventCallback,
        observerModeHandler: AdaptyUiObserverModeHandler?,
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver,
    ) {
        if (!isObserverMode) {
            if (observerModeHandler != null)
                log(WARN) { "$LOG_PREFIX $flowKey You should not pass observerModeHandler if you're using Adapty in Full Mode" }
            performMakePurchase(activity, product, eventListener, personalizedOfferResolver)
        } else {
            if (observerModeHandler != null) {
                log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onPurchaseInitiated begin" }
                observerModeHandler.onPurchaseInitiated(
                    product,
                    paywall,
                    activity,
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onStartPurchase called" }
                        toggleLoading(true)
                    },
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onFinishPurchase called" }
                        toggleLoading(false)
                    },
                )
            } else {
                log(WARN) { "$LOG_PREFIX $flowKey In order to handle purchases in Observer Mode enabled, provide the observerModeHandler!" }
                performMakePurchase(activity, product, eventListener, personalizedOfferResolver)
            }
        }
    }

    private fun performMakePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        eventListener: EventCallback,
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver,
    ) {
        toggleLoading(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase begin" }
        val subscriptionUpdateParams =
            eventListener.onAwaitingSubscriptionUpdateParams(product)
        val isOfferPersonalized = personalizedOfferResolver.resolve(product)
        eventListener.onPurchaseStarted(product)
        Adapty.makePurchase(activity, product, subscriptionUpdateParams, isOfferPersonalized) { result ->
            toggleLoading(false)
            when (result) {
                is AdaptyResult.Success -> {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase success" }
                    eventListener.onPurchaseSuccess(
                        result.value,
                        product,
                    )
                }
                is AdaptyResult.Error -> {
                    val error = result.error
                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey makePurchase error: ${error.message}" }
                    when (error.adaptyErrorCode) {
                        AdaptyErrorCode.USER_CANCELED -> {
                            eventListener.onPurchaseCanceled(product)
                        }
                        else -> {
                            eventListener.onPurchaseFailure(
                                result.error,
                                product,
                            )
                        }
                    }
                }
            }
        }
    }

    fun onRestorePurchases(eventListener: EventCallback) {
        toggleLoading(true)
        log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases begin" }
        eventListener.onRestoreStarted()
        Adapty.restorePurchases { result ->
            toggleLoading(false)
            when (result) {
                is AdaptyResult.Success -> {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases success" }
                    eventListener.onRestoreSuccess(result.value)
                }
                is AdaptyResult.Error -> {
                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey restorePurchases error: ${result.error.message}" }
                    eventListener.onRestoreFailure(result.error)
                }
            }
        }
    }

    private suspend fun loadProducts(
        paywall: AdaptyPaywall,
        loadingFailureCallback: ProductLoadingFailureCallback,
    ): AdaptyResult<List<AdaptyPaywallProduct>> {
        suspend fun load(
            paywall: AdaptyPaywall,
        ): AdaptyResult<List<AdaptyPaywallProduct>> =
            suspendCancellableCoroutine { continuation ->
                log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts begin" }
                Adapty.getPaywallProducts(paywall) { result ->
                    when (result) {
                        is AdaptyResult.Success -> {
                            continuation.resumeWith(Result.success(result))
                            log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts success" }
                        }
                        is AdaptyResult.Error -> {
                            continuation.resumeWith(Result.success(result))
                            log(ERROR) { "$LOG_PREFIX_ERROR $flowKey loadProducts error: ${result.error.message}" }
                        }
                    }
                }
            }

        val productResult = load(paywall)
        return when (productResult) {
            is AdaptyResult.Success -> productResult
            is AdaptyResult.Error -> {
                val shouldRetry =
                    loadingFailureCallback.onLoadingProductsFailure(productResult.error)
                if (shouldRetry) {
                    delay(LOADING_PRODUCTS_RETRY_DELAY)
                    loadProducts(paywall, loadingFailureCallback)
                } else
                    productResult
            }
        }
    }

    fun logShowPaywall(viewConfig: AdaptyUI.LocalizedViewConfiguration) {
        log(VERBOSE) { "$LOG_PREFIX $flowKey logShowPaywall begin" }
        Adapty.logShowPaywall(
            viewConfig.paywall,
            mapOf("paywall_builder_id" to viewConfig.id)
        ) { error ->
            if (error != null) {
                log(ERROR) { "$LOG_PREFIX_ERROR $flowKey logShowPaywall error: ${error.message}" }
            } else {
                log(VERBOSE) { "$LOG_PREFIX $flowKey logShowPaywall success" }
            }
        }
    }

    private fun toggleLoading(show: Boolean) {
        isLoading.value = show
    }

    @Composable
    fun resolveText(stringId: StringId) =
        textResolver.resolve(stringId, texts, products, assets, state)

    fun getTimerStartTimestamp(placementId: String, timerId: String, isPersisted: Boolean): Long? {
        return cacheRepository.getLongValue(getTimerStartTimestampId(placementId, timerId), isPersisted)
    }

    fun setTimerStartTimestamp(placementId: String, timerId: String, value: Long, isPersisted: Boolean) {
        cacheRepository.setLongValue(getTimerStartTimestampId(placementId, timerId), value, isPersisted)
    }

    private fun getTimerStartTimestampId(placementId: String, timerId: String) =
        "${placementId}_timer_${timerId}_start"
}

internal class PaywallViewModelFactory(
    private val vmArgs: PaywallViewModelArgs,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PaywallViewModel::class.java) -> {
                PaywallViewModel(
                    vmArgs.flowKey,
                    vmArgs.isObserverMode,
                    vmArgs.mediaFetchService,
                    vmArgs.cacheRepository,
                    vmArgs.textResolver,
                    vmArgs.userArgs,
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

internal class PaywallViewModelArgs(
    val flowKey: String,
    val isObserverMode: Boolean,
    val mediaFetchService: MediaFetchService,
    val cacheRepository: CacheRepositoryProxy,
    val textResolver: TextResolver,
    val userArgs: UserArgs?,
) {
    companion object {
        fun create(
            flowKey: String,
            userArgs: UserArgs?,
            locale: Locale,
        ) =
            runCatching {
                val mediaFetchService = Dependencies.injectInternal<MediaFetchService>()
                val cacheRepository = Dependencies.injectInternal<CacheRepositoryProxy>()
                val isObserverMode = Dependencies.injectInternal<Boolean>(OBSERVER_MODE)
                val priceFormatter = Dependencies.injectInternal<PriceFormatter>(locale.toString()) {
                    DIObject({ PriceFormatter(locale) })
                }
                val priceConverter = PriceConverter()
                val tagResolver = TagResolver(
                    priceFormatter,
                    priceConverter,
                    userArgs?.tagResolver ?: AdaptyUiTagResolver.DEFAULT,
                )
                val textResolver = TextResolver(tagResolver)
                PaywallViewModelArgs(
                    flowKey,
                    isObserverMode,
                    mediaFetchService,
                    cacheRepository,
                    textResolver,
                    userArgs,
                )
            }.getOrElse { e ->
                log(ERROR) {
                    "$LOG_PREFIX_ERROR $flowKey rendering error: ${e.localizedMessage}"
                }
                null
            }
    }
}

internal class UserArgs(
    val viewConfig: AdaptyUI.LocalizedViewConfiguration,
    val eventListener: AdaptyUiEventListener,
    val personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver,
    val tagResolver: AdaptyUiTagResolver,
    val timerResolver: AdaptyUiTimerResolver,
    val observerModeHandler: AdaptyUiObserverModeHandler?,
    val products: List<AdaptyPaywallProduct>,
    val productLoadingFailureCallback: ProductLoadingFailureCallback,
) {
    companion object {
        fun create(
            viewConfig: AdaptyUI.LocalizedViewConfiguration,
            eventListener: AdaptyUiEventListener,
            personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver,
            tagResolver: AdaptyUiTagResolver,
            timerResolver: AdaptyUiTimerResolver,
            observerModeHandler: AdaptyUiObserverModeHandler?,
            products: List<AdaptyPaywallProduct>?,
            productLoadingFailureCallback: ProductLoadingFailureCallback,
        ) =
            UserArgs(
                viewConfig,
                eventListener,
                personalizedOfferResolver,
                tagResolver,
                timerResolver,
                observerModeHandler,
                products.orEmpty(),
                productLoadingFailureCallback,
            )
    }
}