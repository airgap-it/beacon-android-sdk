# Beacon Android SDK

[![release](https://img.shields.io/jitpack/v/github/airgap-it/beacon-android-sdk)](https://jitpack.io/#airgap-it/beacon-android-sdk)

> Connect Wallets with dApps on Tezos

[Beacon](https://walletbeacon.io) is an implementation of the wallet interaction standard [tzip-10](https://gitlab.com/tzip/tzip/blob/master/proposals/tzip-10/tzip-10.md) which describes the connection of a dApp with a wallet.

## About

The `Beacon Android SDK` provides Android developers with tools useful for setting up communication between native wallets supporting Tezos and dApps that implement [`beacon-sdk`](https://github.com/airgap-it/beacon-sdk).

## Installation

To add `Beacon Android SDK` into your project:

  1. Make sure the [JitPack](https://jitpack.io/) repository is added to your root `build.gradle` file:
  ```groovy
  allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
  }
  ```

  2. Add the dependency:
  ```groovy
  def beaconVersion = "1.0.0"

  implementation "com.github.airgap-it:beacon-android-sdk:$beaconVersion"
  ```

<!-- TODO: ## Documentation -->

## Project Overview

The project consists of the following modules:

- `core` - the main library module (Kotlin)
- `demo` - an example application (Kotlin & Java)

## Examples

The snippets below show how to quickly setup listening for incoming Beacon messages in Kotlin with coroutines. 

For more examples or examples of how to use the SDK without coroutines or in Java, please see our `demo` app (WIP).

### Create a Beacon client and listen for incoming messages

```kotlin
import it.airgap.beaconsdk.core.client.BeaconClient

class MainActivity : AppCompatActivity() {
  lateinit var client: BeaconClient

  // ...

  suspend fun listenForBeaconMessages() {
    // create a Beacon client
    client = BeaconClient("My App")

    myCoroutineScope.launch {
      // subscribe to a message Flow
      client.connect().collect { /* process messages */ }
    }
  }
}
```

## Development

The project is built with [Gradle](https://gradle.org/).


### Product Flavors
There are the following product flavors configured:

- `prod` - the production ready version
- `mock` - provides mock implementations, used only for unit tests

### Build

#### Android Studio

Before building the project in Android Studio make sure the active build variant uses the `prod` flavor.

#### Command Line

Build all modules:
```
$ ./gradlew assembleProd
```

Build a single module:
```
$ ./gradlew :${module}:assembleProd
```

### Run Tests

#### Android Studio

Before running the tests in Android Studio make sure the active build variant uses the `mock` flavor.

#### Command Line

```
$ ./gradlew testMock{Release|Debug}UnitTest
```

---
## Related Projects

[Beacon SDK](https://github.com/airgap-it/beacon-sdk) - an SDK for web developers (dApp & wallet)

[Beacon iOS SDK](https://github.com/airgap-it/beacon-ios-sdk) - an SDK for iOS developers (wallet)
