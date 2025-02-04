## ⚠️ This repository is deprecated because AdaptyUI is now part of [AdaptySDK](https://github.com/adaptyteam/AdaptySDK-Android). Please consider updating. ⚠️

<h1 align="center" style="border-bottom: none">
<b>
    <a href="https://adapty.io/?utm_source=github&utm_medium=referral&utm_campaign=AdaptyUI-Android">
        <img src="https://adapty-portal-media-production.s3.amazonaws.com/github/logo-adapty-new.svg">
    </a>
</b>
<br>Adapty UI
</h1>

<p align="center">
<a href="https://discord.gg/subscriptions-hub"><img src="https://img.shields.io/badge/Adapty-discord-purple"></a>
<a href="https://maven-badges.herokuapp.com/maven-central/io.adapty/android-ui"><img src="https://maven-badges.herokuapp.com/maven-central/io.adapty/android-ui/badge.svg"></a>
<a href="https://github.com/adaptyteam/AdaptyUI-Android/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-brightgreen.svg"></a>
</p>

**AdaptyUI** is an open-source framework that is an extension to the Adapty SDK that allows you to easily add purchase screens to your application. It’s 100% open-source, native, and lightweight.

### [1. Fetching Paywalls & ViewConfiguration](https://docs.adapty.io/docs/paywall-builder-fetching)

Paywall can be obtained in the way you are already familiar with:

```kotlin
Adapty.getPaywall("YOUR_PAYWALL_ID") { result ->
    when (result) {
        is AdaptyResult.Success -> {
            val paywall = result.value
            // the requested paywall
        }
        is AdaptyResult.Error -> {
            val error = result.error
            // handle the error
        }
    }
}
```

After fetching the paywall call the `Adapty.getViewConfiguration(paywall, locale)` method to load the view configuration:

```kotlin
Adapty.getViewConfiguration(paywall, locale) { result ->
    when(result) {
        is AdaptyResult.Success -> {
            val viewConfiguration = result.value
            // use loaded configuration
        }
        is AdaptyResult.Error -> {
            val error = result.error
            // handle the error
        }
    }
}
```

### [2. Presenting Visual Paywalls](https://docs.adapty.io/docs/paywall-builder-presenting-android)

In order to display the visual paywall on the device screen, you must first configure it. To do this, call the method `AdaptyUI.getPaywallView()` or create the `AdaptyPaywallView` directly:

```kotlin
val paywallView = AdaptyUI.getPaywallView(
    activity,
    paywall,
    products,
    viewConfiguration,
    AdaptyPaywallInsets.of(topInset, bottomInset),
    eventListener,
)

//======= OR =======

val paywallView =
    AdaptyPaywallView(activity) // or retrieve it from xml
...
with(paywallView) {
    setEventListener(eventListener)
    showPaywall(
        paywall,
        products,
        viewConfiguration,
        AdaptyPaywallInsets.of(topInset, bottomInset),
    )
}

```

After the object has been successfully created, you can add it to the view hierarchy and display on the screen of the device.

### 3. Full Documentation and Next Steps

We recommend that you read the [full documentation](https://docs.adapty.io/docs/paywall-builder-getting-started). If you are not familiar with Adapty, then start [here](https://docs.adapty.io/docs).

## Contributing

- Feel free to open an issue, we check all of them or drop us an email at [support@adapty.io](mailto:support@adapty.io) and tell us everything you want.
- Want to suggest a feature? Just contact us or open an issue in the repo.

## Like AdaptyUI?

So do we! Feel free to star the repo ⭐️⭐️⭐️ and make our developers happy!

## License

AdaptyUI is available under the MIT license. [Click here](https://github.com/adaptyteam/AdaptyUI-Android/blob/main/LICENSE) for details.

---
