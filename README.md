# Web2Wave

Web2Wave is a lightweight Kotlin package that provides a simple interface for managing user subscriptions and properties through a REST API.

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

```java

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

In you app-level `build.gradle.kts` file:

```java
dependencies {
        implementation("com.github.web2wave:web2wave_kotlin:1.0.1")
}
```

## Setup

Before using Web2Wave, you need to configure the base URL and API key:

```java
Web2Wave.initWith("your-api-key")
```

## Usage

The library works with the network, make sure that the calls are not made on the main thread.

### Checking Subscription Status

```java

    // Fetch subscriptions
    val status = Web2Wave.fetchSubscriptions(userID = "user123")

    // Check if user has an active subscription
    val isActive = Web2Wave.hasActiveSubscription(userID = "user123")

```

### Managing User Properties

```java
// Fetch user properties
    val properties = Web2Wave.fetchUserProperties(userID = "user123")
        print("User properties: \(properties)")


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

### Managing third-party profiles
```java

    // Save Adapty profileID
    val result = Web2Wave.setAdaptyProfileID(
        appUserID = "user123",
        adaptyProfileID = "{adaptyProfileID}"
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
        revenueCatProfileID = "{revenueCatProfileID}"
    )

    // Save Qonversion profileID
    val qonversionResult = Web2Wave.setQonversionProfileID(
        appUserID = "user123",
        qonversionProfileID = "{qonversionProfileID}"
    )

```

## API Reference

### `Web2Wave`

The singleton instance of the Web2Wave client.

### Methods

#### `fun fetchSubscriptionStatus(appUserID: String) : Map<String, Any>?`
Fetches the subscription status for a given user ID.

#### `fun fetchSubscriptions(appUserID: String) : List<Map<String, Any>>?`
Fetches all subscriptionsfor a given user ID.

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

