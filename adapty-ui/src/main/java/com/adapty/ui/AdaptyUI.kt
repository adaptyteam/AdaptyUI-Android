@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import com.adapty.Adapty
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.DEFAULT_PAYWALL_TIMEOUT
import com.adapty.internal.utils.HashingHelper
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.utils.TimeInterval
import com.adapty.utils.days
import com.adapty.ui.internal.LOG_PREFIX
import com.adapty.ui.internal.ViewConfigurationMapper
import com.adapty.ui.internal.cache.CacheCleanupService
import com.adapty.ui.internal.cache.CacheFileManager
import com.adapty.ui.internal.cache.MediaCacheConfigManager
import com.adapty.ui.internal.cache.MediaDownloader
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.cache.MediaSaver
import com.adapty.ui.internal.cache.SingleMediaHandlerFactory
import com.adapty.ui.internal.log
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiPersonalizedOfferResolver
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.ResultCallback

public object AdaptyUI {

    /**
     * Right after receiving [ViewConfiguration], you can create the corresponding
     * [AdaptyPaywallView] to display it afterwards.
     *
     * This method should be called only on UI thread.
     *
     * @param[activity] An [Activity] instance.
     *
     * @param[viewConfiguration] An [ViewConfiguration] object containing information
     * about the visual part of the paywall. To load it, use the [AdaptyUI.getViewConfiguration] method.
     *
     * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
     * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
     * automatically fetch the required products.
     *
     * @param[insets] In case the status bar or navigation bar overlap the view, you can pass
     * an [AdaptyPaywallInsets] object.
     * If the status bar doesn't overlap the [AdaptyPaywallView], the [top][AdaptyPaywallInsets.top]
     * value should be 0.
     * If the navigation bar doesn't overlap the [AdaptyPaywallView], the [bottom][AdaptyPaywallInsets.bottom]
     * value should be 0.
     * If none of them do, you can pass [AdaptyPaywallInsets.NONE].
     *
     * @param[eventListener] An object that implements the [AdaptyUiEventListener] interface.
     * Use it to respond to different events happening inside the purchase screen.
     * Also you can extend [AdaptyUiDefaultEventListener] so you don't need to override all the methods.
     *
     * @param[personalizedOfferResolver] In case you want to indicate whether the price is personalized ([read more](https://developer.android.com/google/play/billing/integrate#personalized-price)),
     * you can implement [AdaptyUiPersonalizedOfferResolver] and pass your own logic
     * that maps [AdaptyPaywallProduct] to `true`, if the price of the product is personalized, otherwise `false`.
     *
     * @param[tagResolver] If you are going to use custom tags functionality, pass the resolver function here.
     *
     * @param[observerModeHandler] If you use Adapty in [Observer mode](https://adapty.io/docs/observer-vs-full-mode),
     * pass the [AdaptyUiObserverModeHandler] implementation to handle purchases on your own.
     *
     * @return An [AdaptyPaywallView] object, representing the requested paywall screen.
     */
    @JvmStatic
    @JvmOverloads
    @UiThread
    public fun getPaywallView(
        activity: Activity,
        viewConfiguration: ViewConfiguration,
        products: List<AdaptyPaywallProduct>?,
        insets: AdaptyPaywallInsets,
        eventListener: AdaptyUiEventListener,
        personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver = AdaptyUiPersonalizedOfferResolver.DEFAULT,
        tagResolver: AdaptyUiTagResolver = AdaptyUiTagResolver.DEFAULT,
        observerModeHandler: AdaptyUiObserverModeHandler? = null,
    ): AdaptyPaywallView {
        return AdaptyPaywallView(activity).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            setEventListener(eventListener)
            observerModeHandler?.let(::setObserverModeHandler)
            showPaywall(
                viewConfiguration,
                products,
                insets,
                personalizedOfferResolver,
                tagResolver,
            )
        }
    }

    /**
     * If you are using the [Paywall Builder](https://adapty.io/docs/adapty-paywall-builder),
     * you can use this method to get a configuration object for your paywall.
     *
     * Should not be called before [Adapty.activate]
     *
     * @param[paywall] The [AdaptyPaywall] for which you want to get a configuration.
     *
     * @param[loadTimeout] This value limits the timeout for this method. The minimum value is 1 second.
     * If a timeout is not required, you can pass [TimeInterval.INFINITE].
     *
     * @param[callback] A result containing the [ViewConfiguration] object.
     *
     * @see <a href="https://adapty.io/docs/display-pb-paywalls">Display paywalls designed with Paywall Builder</a>
     */
    @JvmStatic
    @JvmOverloads
    public fun getViewConfiguration(
        paywall: AdaptyPaywall,
        loadTimeout: TimeInterval = DEFAULT_PAYWALL_TIMEOUT,
        callback: ResultCallback<ViewConfiguration>
    ) {
        Adapty.getViewConfiguration(paywall, loadTimeout) { result ->
            callback.onResult(result.map { rawConfig ->
                viewConfigMapper.map(rawConfig, paywall)
            })
        }
    }

    private fun preloadMedia(rawConfig: Map<String, Any>) {
        runCatching {
            val (configId, urls) = viewConfigMapper.mapToMediaUrls(rawConfig)
            mediaFetchService.preloadMedia(configId, urls)
        }
    }

    public sealed class Action {
        public object Close : Action()
        public class OpenUrl(public val url: String) : Action()
        public class Custom(public val customId: String): Action()
    }

    public class ViewConfiguration internal constructor(
        @get:JvmSynthetic internal val id: String,
        @get:JvmSynthetic internal val paywall: AdaptyPaywall,
        public val isHard: Boolean,
        @get:JvmSynthetic internal val templateId: String?,
        @get:JvmSynthetic internal val mainImageRelativeHeight: Float,
        private val defaultLocalization: String?,
        private val assets: Map<String, Asset>,
        private val localizations: Map<String, Localization>,
        private val styles: Map<String, Style?>,
    ) {

        internal class Style(
            val featureBlock: FeatureBlock?,
            val productBlock: ProductBlock,
            val footerBlock: FooterBlock?,
            val items: Map<String, Component>,
        )

        internal class FeatureBlock(
            val type: Type,
            val orderedItems: List<Component>,
        ) {
            enum class Type { LIST, TIMELINE }
        }
        internal class ProductBlock(
            val type: Type,
            val mainProductIndex: Int,
            val initiatePurchaseOnTap: Boolean,
            val products: Map<String, Component.ProductObject>,
        ) {
            enum class Type { SINGLE, VERTICAL, HORIZONTAL }
        }
        internal class FooterBlock(
            val orderedItems: List<Component>,
        )

        /**
         * @suppress
         */
        public sealed class Component {

            public sealed class Text(
                internal val horizontalAlign: HorizontalAlign,
            ) : Component() {

                public class Single internal constructor(
                    internal val stringId: String,
                    internal val fontId: String,
                    internal val size: Float?,
                    internal val textColorId: String?,
                    horizontalAlign: HorizontalAlign,
                ): Text(horizontalAlign)

                public class Multiple internal constructor(
                    internal val items: List<Item>,
                    horizontalAlign: HorizontalAlign,
                ): Text(horizontalAlign)
                public sealed class Item {
                    public class Text internal constructor(
                        internal val stringId: String,
                        internal val fontId: String,
                        internal val size: Float?,
                        internal val textColorId: String?,
                        internal val horizontalAlign: HorizontalAlign,
                    ) : Item()

                    public class Image internal constructor(
                        internal val imageId: String,
                        internal val tintColorId: String?,
                        internal val width: Float,
                        internal val height: Float,
                    ): Item()
                    public object NewLine: Item()
                    public class Space internal constructor(internal val value: Float): Item()
                    public class BulletedText internal constructor(
                        internal val bullet: Bullet,
                        internal val space: Space?,
                        internal val text: Text,
                    ) : Item() {
                        public sealed class Bullet

                        public class ImageBullet internal constructor(internal val image: Image): Bullet()
                        public class TextBullet internal constructor(internal val text: Text): Bullet()
                    }
                }
            }

            public class Shape internal constructor(
                internal val backgroundAssetId: String?,
                internal val type: Type,
                internal val border: Border?,
            ): Component() {
                public sealed class Type {
                    public class Rectangle internal constructor(internal val cornerRadius: CornerRadius): Type()
                    public object Circle: Type()
                    public class RectWithArc internal constructor(internal val arcHeight: Float): Type() {
                        internal companion object {
                            const val ABS_ARC_HEIGHT = 20f
                        }
                    }
                }

                public sealed class CornerRadius {
                    public object None: CornerRadius()
                    public class Same internal constructor(internal val value: Float): CornerRadius()
                    public class Different internal constructor(
                        internal val topLeft: Float,
                        internal val topRight: Float,
                        internal val bottomRight: Float,
                        internal val bottomLeft: Float,
                    ) : CornerRadius()
                }

                internal class Border(
                    val assetId: String,
                    val thickness: Float,
                )
            }

            public class Button internal constructor(
                internal val shape: Shape?,
                internal val selectedShape: Shape?,
                internal val title: Text?,
                internal val selectedTitle: Text?,
                internal val align: Align,
                internal val action: Action?,
                internal val isVisible: Boolean,
                internal val transitionIn: Transition?,
            ): Component() {
                public sealed class Action {
                    public object Close: Action()
                    public object Restore: Action()
                    public class OpenUrl internal constructor(internal val urlId: String): Action()
                    public class Custom internal constructor(internal val customId: String): Action()
                }

                internal enum class Align {
                    LEADING, TRAILING, CENTER, FILL
                }

                public sealed class Transition(
                    internal val durationMillis: Long,
                    internal val startDelayMillis: Long,
                    internal val interpolatorName: String,
                ) {
                    public class Fade(
                        durationMillis: Long,
                        startDelayMillis: Long,
                        interpolatorName: String,
                    ): Transition(durationMillis, startDelayMillis, interpolatorName)
                }
            }

            public class Reference internal constructor(
                internal val assetId: String,
            ): Component()

            public class ProductObject internal constructor(
                internal val productId: String,
                internal val properties: Map<String, Component>,
            ): Component()

            public class CustomObject internal constructor(
                internal val type: String,
                internal val properties: List<Pair<String, Component>>,
            ): Component()
        }

        internal enum class HorizontalAlign { LEFT, CENTER, RIGHT }

        /**
         * @suppress
         */
        public sealed class Asset {

            public class Color internal constructor(
                @ColorInt internal val value: Int,
            ): Filling.Local()

            public class Gradient internal constructor(
                internal val type: Type,
                private val values: List<Value>,
                internal val points: Points,
            ): Filling.Local() {

                internal val colors: IntArray get() = values.map { it.color.value }.toIntArray()
                internal val positions: FloatArray get() = values.map { it.p }.toFloatArray()
                internal enum class Type { LINEAR, RADIAL, CONIC }

                internal class Value(
                    val p: Float,
                    val color: Color,
                )

                internal class Points(
                    val x0: Float,
                    val y0: Float,
                    val x1: Float,
                    val y1: Float,
                ) {
                    operator fun component1(): Float = x0
                    operator fun component2(): Float = y0
                    operator fun component3(): Float = x1
                    operator fun component4(): Float = y1
                }
            }

            public class Font internal constructor(
                internal val familyName: String,
                internal val resources: List<String>,
                internal val weight: Int,
                internal val isItalic: Boolean,
                internal val size: Float?,
                internal val horizontalAlign: HorizontalAlign?,
                @ColorInt internal val color: Int?,
            ): Asset()

            public class Image internal constructor(
                internal val source: Source,
            ): Filling.Local() {

                public sealed class Source {
                    public class File(internal val file: java.io.File): Source()
                    public class Base64Str(internal val imageBase64: String?): Source()
                }

                internal enum class Dimension { WIDTH, HEIGHT }

                internal enum class ScaleType { FIT_MIN, FIT_MAX }
            }

            public class RemoteImage internal constructor(
                internal val url: String,
                internal val preview: Image?,
            ): Filling()

            public sealed class Filling: Asset() {
                public sealed class Local: Filling()
            }
        }

        internal class Localization(
            val strings: Map<String, Str>,
            val assets: Map<String, Asset>,
        ) {
            class Str(
                val value: String,
                val fallback: String?,
                val hasTags: Boolean,
            )
        }

        internal fun <T : Asset> getAsset(assetId: String): T? {
            val localeStr = defaultLocalization
            return (localizations[localeStr]?.assets?.get(assetId)
                ?: localizations[defaultLocalization]?.assets?.get(assetId) ?: assets[assetId]) as? T
        }

        internal fun getString(strId: String): Localization.Str? {
            val localeStr = defaultLocalization
            return (localizations[localeStr]?.strings?.get(strId)
                ?: localizations[defaultLocalization]?.strings?.get(strId))
        }

        internal fun getStyle(styleId: String): Style? = styles[styleId]

        internal fun hasStyle(styleId: String): Boolean = styles[styleId] != null
    }

    @JvmStatic
    public fun configureMediaCache(config: MediaCacheConfiguration) {
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# configure: diskStorageSizeLimit = ${config.diskStorageSizeLimit}, discCacheValidityTime = ${config.discCacheValidityTime}" }
        val cacheConfigManager = runCatching { Dependencies.injectInternal<MediaCacheConfigManager>() }.getOrNull() ?: run {
            log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# couldn't be configured. Adapty was not initialized" }
            return
        }
        cacheConfigManager.currentCacheConfig = config
    }

    public class MediaCacheConfiguration private constructor(
        public val diskStorageSizeLimit: Long,
        public val discCacheValidityTime: TimeInterval,
    ) {

        private companion object {
            private const val DEFAULT_DISK_STORAGE_SIZE_LIMIT_BYTES = 100L * 1024 * 1024
            private val DEFAULT_DISK_CACHE_VALIDITY_TIME = 7.days
        }

        public class Builder {

            private var diskStorageSizeLimit: Long = DEFAULT_DISK_STORAGE_SIZE_LIMIT_BYTES

            private var discCacheValidityTime: TimeInterval = DEFAULT_DISK_CACHE_VALIDITY_TIME

            public fun overrideDiskStorageSizeLimit(limitInBytes: Long): Builder {
                diskStorageSizeLimit = limitInBytes
                return this
            }

            public fun overrideDiskCacheValidityTime(time: TimeInterval): Builder {
                discCacheValidityTime = time
                return this
            }

            public fun build(): MediaCacheConfiguration =
                MediaCacheConfiguration(diskStorageSizeLimit, discCacheValidityTime)
        }
    }

    @JvmStatic
    public fun clearMediaCache(strategy: ClearCacheStrategy) {
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# clear: ${strategy.name}" }
        val cacheCleanupService = runCatching { Dependencies.injectInternal<CacheCleanupService>() }.getOrNull() ?: run {
            log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# couldn't clear cache. Adapty was not initialized" }
            return
        }

        when (strategy) {
            ClearCacheStrategy.CLEAR_ALL -> cacheCleanupService.clearAll()
            ClearCacheStrategy.CLEAR_EXPIRED_ONLY -> cacheCleanupService.clearExpired()
        }
    }

    public enum class ClearCacheStrategy {
        CLEAR_ALL,
        CLEAR_EXPIRED_ONLY
    }

    init {
        initAllDeps()
    }

    private fun initAllDeps() {
        Dependencies.contribute(
            setOf(
                ViewConfigurationMapper::class to Dependencies.singleVariantDiObject({
                    ViewConfigurationMapper()
                }),
                MediaCacheConfigManager::class to Dependencies.singleVariantDiObject({
                    MediaCacheConfigManager()
                }),
            )
        )

        val appContext = runCatching { Dependencies.injectInternal<Context>() }.getOrNull()

        if (appContext == null) {
            Dependencies.onInitialDepsCreated = {
                contributeDepsOnAdaptyReady(Dependencies.injectInternal<Context>())
            }
        } else {
            contributeDepsOnAdaptyReady(appContext)
        }
    }

    private fun contributeDepsOnAdaptyReady(appContext: Context) {
        Dependencies.contribute(
            listOf(
                CacheFileManager::class to Dependencies.singleVariantDiObject({
                    CacheFileManager(appContext, Dependencies.injectInternal<HashingHelper>())
                }),
                CacheCleanupService::class to Dependencies.singleVariantDiObject({
                    CacheCleanupService(
                        Dependencies.injectInternal<CacheFileManager>(),
                        Dependencies.injectInternal<MediaCacheConfigManager>(),
                    )
                }),
                MediaFetchService::class to Dependencies.singleVariantDiObject({
                    val cacheFileManager = Dependencies.injectInternal<CacheFileManager>()
                    val mediaDownloader = MediaDownloader()
                    val mediaSaver = MediaSaver(cacheFileManager)
                    val singleMediaHandlerFactory =
                        SingleMediaHandlerFactory(
                            mediaDownloader,
                            mediaSaver,
                            cacheFileManager,
                            Dependencies.injectInternal<CacheCleanupService>(),
                        )
                    MediaFetchService(singleMediaHandlerFactory)
                }),
            )
        )
    }

    private val viewConfigMapper: ViewConfigurationMapper by inject()

    private val mediaFetchService: MediaFetchService by inject()
}