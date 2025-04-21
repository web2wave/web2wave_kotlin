# Web2Wave

Web2Wave is a lightweight Kotlin package that provides a simple interface for managing user
subscriptions and properties through a REST API.

## Features

- Fetch subscription status for users
- Check for active subscriptions
- Manage user properties
- Set third-parties profiles
- Thread-safe singleton design
- Built-in error handling

## Installation

### Gradle Installation

Add the following to your `settings.gradle.kts` file:

```kotlin

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

In you app-level `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.github.web2wave:web2wave_kotlin:1.0.1")
}
```

## Setup

Before using Web2Wave, you need to configure the base URL and API key:

```kotlin
Web2Wave.initWith("your-api-key")
```

## Usage

The library works with the network, make sure that the calls are not made on the main thread.

### Checking Subscription Status

```kotlin

// Fetch subscriptions
val status = Web2Wave.fetchSubscriptions(userID = "user123")

// Check if user has an active subscription
val isActive = Web2Wave.hasActiveSubscription(userID = "user123")

```

### Managing User Properties

```kotlin
// Fetch user properties
val properties = Web2Wave.fetchUserProperties(userID = "user123")
print("User properties: $properties")


// Update a user property
val result = Web2Wave.updateUserProperty(
    userID = "user123",
    property = "preferredTheme",
    value = "dark"
)
when {
    result.isSuccess -> {
        print("Property updated successfully")
    }

    result.isFailure -> {
        print("Failed to update property: ${result.exceptionOrNull()}")
    }
}

```

### External Subscription Cancel/Refund/Charge

```kotlin
  // Cancel subscription in external Stripe/Paddle/PayPal
val resultCancelSubscription = Web2Wave.cancelSubscription(
    paySystemId = "sub_1PzNJzCsRq5tBi2bbfNsAf86 or I-H7HC902MYM49",
    comment = "may be null"
)

when {
    resultCancelSubscription.isSuccess -> {
        print("Subscription canceled")
    }

    resultCancelSubscription.isFailure -> {
        print("Failed to cancel subscription with error: ${resultCancelSubscription.exceptionOrNull()}")
    }
}


// Refund subscription with invoiceID in external Stripe/Paddle/PayPal
val resultRefundSubscription = Web2Wave.refundSubscription(
    paySystemId = "sub_1PzNJzCsRq5tBi2bbfNsAf86 or I-H7HC902MYM49",
    invoiceId = "your_invoice_id",
    comment = "may be null"
)

when {
    resultRefundSubscription.isSuccess -> {
        print("Subscription refunded")
    }

    resultRefundSubscription.isFailure -> {
        print("Failed to refund subscription with error: ${resultRefundSubscription.exceptionOrNull()}")
    }
}

val resultChargeUser = Web2Wave.chargeUser(
    web2waveUserId = "User123",
    priceId = 22057
)

when {
    resultChargeUser.isSuccess -> {
        print("User charged")
    }

    resultChargeUser.isFailure -> {
        print("Failed to charge user with error: ${resultChargeUser.exceptionOrNull()}")
    }
}

```

### Managing third-party profiles

```kotlin

// Save Adapty profileID
val result = Web2Wave.setAdaptyProfileID(
    appUserID = "user123",
    adaptyProfileID = "adaptyProfileID"
)

when {
    result.isSuccess -> {
        print("ProfileID saved")
    }
    result.isFailure -> {
        print("Failed to save profileID: ${result.exceptionOrNull()}")
    }
}

// Save Revenue Cat profileID
val revResult = Web2Wave.setRevenuecatProfileID(
    appUserID = "user123",
    revenueCatProfileID = "revenueCatProfileID"
)

// Save Qonversion profileID
val qonversionResult = Web2Wave.setQonversionProfileID(
    appUserID = "user123",
    qonversionProfileID = "qonversionProfileID"
)

```

### Working with quiz or landing web page

```kotlin
  //Extend Web2WaveWebListener class to receive events
class EventListener extends Web2WaveWebListener {

    override fun onEvent(event: String, data: Map<String, Any>?) {
        print("onEvent: $event, data: ${data.toString()}")
    }

    override fun onClose(data: Map<String, Any>?) {
        print("onClose: data: ${data.toString()}")
        Web2Wave.closeWebPage()
    }

    override fun onQuizFinished(data: Map<String, Any>?) {
        print("onQuizFinished: data: ${data.toString()}")
        Web2Wave.closeWebPage()
    }
}

//Open web page with your url
Web2Wave.showWebView(
    fragmentManager: FragmentManager,
    url: String,
    listener: Web2WaveWebListener,
    topOffset: Int,
    bottomOffset: Int
)

//Close web page
Web2Wave.closeWebView()
```

## API Reference

### `Web2Wave`

The singleton instance of the Web2Wave client.

### Methods

#### `fun fetchSubscriptionStatus(appUserID: String) : Map<String, Any>?`

Fetches the subscription status for a given user ID.

#### `fun fetchSubscriptionStatus(appUserID: String) : List<Map<String, Any>>?`

Fetches all subscriptions for a given user ID.

#### `fun hasActiveSubscription(appUserID: String) : Boolean`

Checks if the user has an active subscription (including trial status).

#### `fun fetchUserProperties(appUserID: String) : Map<String, Any>?`

Retrieves all properties associated with a user.

#### `fun updateUserProperty(appUserID: String, property: String, value: String) : Result<Unit>`

Updates a specific property for a user.

#### `fun setRevenuecatProfileID(appUserID: String, revenueCatProfileID: String) : Result<Unit>`

Set Revenuecat profileID

#### `fun setAdaptyProfileID(appUserID: String, adaptyProfileID: String) : Result<Unit>`

Set Adapty profileID

#### `fun setQonversionProfileID(appUserID: String, qonversionProfileID: String) : Result<Unit>`

Set Qonversion ProfileID

## License

MIT

## Author

Aleksandr Filpenko

