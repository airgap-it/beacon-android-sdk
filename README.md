# Beacon Android SDK

[![stable](https://img.shields.io/github/v/tag/airgap-it/beacon-android-sdk?label=stable&sort=semver)](https://github.com/airgap-it/beacon-android-sdk/releases)
[![latest](https://img.shields.io/github/v/tag/airgap-it/beacon-android-sdk?color=orange&include_prereleases&label=latest)](https://github.com/airgap-it/beacon-android-sdk/releases)
[![release](https://img.shields.io/jitpack/v/github/airgap-it/beacon-android-sdk)](https://jitpack.io/#airgap-it/beacon-android-sdk)
[![documentation](https://img.shields.io/badge/documentation-online-brightgreen.svg)](https://docs.walletbeacon.io/wallet/getting-started/android/installation)
[![license](https://img.shields.io/github/license/airgap-it/beacon-android-sdk)](https://github.com/airgap-it/beacon-android-sdk/blob/master/LICENSE)

> Connect Wallets with dApps on Tezos

[Beacon](https://walletbeacon.io) is an implementation of the wallet interaction standard [tzip-10](https://gitlab.com/tzip/tzip/blob/master/proposals/tzip-10/tzip-10.md) which describes the connection of a dApp with a wallet.

## About

The `Beacon Android SDK` provides Android developers with tools useful for setting up communication between native wallets supporting Tezos and dApps that implement [`beacon-sdk`](https://github.com/airgap-it/beacon-sdk).

## Installation

To add `Beacon Android SDK` into your project:

  1. Make sure the [JitPack](https://jitpack.io/) repository is included in your root `build.gradle` file:

  #### Groovy
  ```groovy
  allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
  }
  ```

  #### Kotlin
  ```kotlin
  allprojects {
    repositories {
      ...
      maven("https://jitpack.io")
    }
  }
  ```

  2. Add the dependencies:

  #### Groovy
  ```groovy
  dependencies {
    def beacon_version = "3.1.0"

    // REQUIRED, core
    implementation "com.github.airgap-it.beacon-android-sdk:core:$beacon_version"
  
    // optional, client-wallet
    implementation "com.github.airgap-it.beacon-android-sdk:client-wallet:$beacon_version"
    // optional, client-wallet-compat
    implementation "com.github.airgap-it.beacon-android-sdk:client-wallet-compat:$beacon_version"
  
    // optional, blockchain-substrate
    implementation "com.github.airgap-it.beacon-android-sdk:blockchain-substrate:$beacon_version"
    // optional, blockchain-tezos
    implementation "com.github.airgap-it.beacon-android-sdk:blockchain-tezos:$beacon_version"
  
    // optional, transport-p2p-matrix
    implementation "com.github.airgap-it.beacon-android-sdk:transport-p2p-matrix:$beacon_version"
  
    ---

    // alternatively, all modules
    implementation "com.github.airgap-it:beacon-android-sdk:$beacon_version"
  }
  ```

  #### Kotlin
  ```kotlin
  dependencies {
    val beaconVersion = "3.1.0"
  
    // REQUIRED, core
    implementation("com.github.airgap-it.beacon-android-sdk:core:$beaconVersion")
  
    // optional, client-wallet
    implementation("com.github.airgap-it.beacon-android-sdk:client-wallet:$beaconVersion")
    // optional, client-wallet-compat
    implementation("com.github.airgap-it.beacon-android-sdk:client-wallet-compat:$beaconVersion")
  
    // optional, blockchain-substrate
    implementation("com.github.airgap-it.beacon-android-sdk:blockchain-substrate:$beaconVersion")
    // optional, blockchain-tezos
    implementation("com.github.airgap-it.beacon-android-sdk:blockchain-tezos:$beaconVersion")
  
    // optional, transport-p2p-matrix
    implementation("com.github.airgap-it.beacon-android-sdk:transport-p2p-matrix:$beaconVersion")
  
    ---
  
    // alternatively, all modules
    implementation("com.github.airgap-it:beacon-android-sdk:$beaconVersion")
  }
  ```
### Proguard and R8

`Beacon Android SDK` internally uses various libraries that may require custom ProGuard rules. If you're using ProGuard or R8, please follow the guides listed below to make sure your app works correctly after obfuscation:

- [ProGuard rules for Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization#android)
- [ProGuard rules for LazySodium](https://github.com/terl/lazysodium-java/wiki/installation#proguard)

### Troubleshooting

See the list of known issues and how to fix them if you run into problems after adding the dependencies.

- `Native library (com/sun/jna/xxxxx/libjnidispatch.so) not found in resource path`

    Add the `"net.java.dev.jna:jna:x.y.z@aar"` dependency **and exclude the `net.java.dev.jna` group from the Beacon dependencies**.
  
    #### Groovy
    ```groovy
    def withoutJna = { exclude group: "net.java.dev.jna" }
    
    implementation "com.github.airgap-it.beacon-android-sdk:core:$beacon_version", withoutJna
    implementation "com.github.airgap-it.beacon-android-sdk:client-wallet:$beacon_version", withoutJna 
    ...
  
    def jna_version = "5.9.0"
    
    implementation "net.java.dev.jna:jna:$jna_version@aar"
    ```
  
    #### Kotlin
    ```kotlin
    fun ModuleDependency.excludeJna(): ModuleDependency = apply {
        exclude(group = "net.java.dev.jna")
    }
    
    implementation("com.github.airgap-it.beacon-android-sdk:core:$beaconVersion") { withoutJna() }
    implementation("com.github.airgap-it.beacon-android-sdk:client-wallet:$beaconVersion") { withoutJna() }
    ...
    
    val jnaVersion = "5.9.0"
  
    implementation("net.java.dev.jna:jna:$jnaVersion@aar")
    ```

## Documentation

The documentation can be found [here](https://docs.walletbeacon.io/).

## Project Overview

The project consists of the following modules:

### Core

Core modules are the basis for other modules. They are required for the SDK to work as expected.

| Module  | Description            | Dependencies | Required by                                                                                                                                          |
| ------- | ---------------------- | ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `:core` | Base for other modules | ✖️           | `:client-wallet` <br /> `:client-wallet-compat` <br /><br /> `:blockchain-substrate` <br /> `:blockchain-tezos` <br /><br /> `:transport-p2p-matrix` |

### Client

Client modules ship with Beacon implementations for different parts of the network.

| Module                  | Description                                                                                                                                | Dependencies                    | Required by             |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------- | ----------------------- |
| `:client-wallet`        | Beacon implementation for wallets                                                                                                          | `:core`                         | `:client-wallet-compat` |
| `:client-wallet-compat` | Provides a supplementary interface for `:client-wallet` for use without [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) | `:core` <br /> `:client-wallet` | ✖️                      |

### Blockchain

Blockchain modules provide support for different blockchains.

| Module                  | Description                   | Dependencies | Required by |
| ----------------------- | ----------------------------- | ------------ | ----------- |
| `:blockchain-substrate` | Substrate specific components | `:core`      | ✖️          |
| `:blockchain-tezos`     | Tezos specific components     | `:core`      | ✖️          |

### Transport

Transport modules provide various interfaces used to establish connection between Beacon clients.

| Module                  | Description                                                                              | Dependencies | Required by |
| ----------------------- | ---------------------------------------------------------------------------------------- | ------------ | ----------- |
| `:transport-p2p-matrix` | Beacon P2P implementation which uses [Matrix](https://matrix.org/) for the communication | `:core`      | ✖️          |

### Demo

Demo modules provide examples of how to use the library. 

| Module  | Description         |
| ------- | ------------------- |
| `:demo` | Example application |

## Examples

The snippets below show how to quickly setup listening for incoming Beacon messages in Kotlin with coroutines. 

For more examples or examples of how to use the SDK without coroutines or in Java, please see our `demo` app (WIP).

### Create a Beacon wallet client and listen for incoming requests

```kotlin
import it.airgap.beaconsdk.blockchain.substrate.substrate
import it.airgap.beaconsdk.blockchain.tezos.tezos
import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix

class MainActivity : AppCompatActivity() {
  lateinit var client: BeaconWalletClient

  // ...

  suspend fun listenForBeaconMessages() {
    // create a wallet Beacon client that can listen for Substrate and Tezos messages via Matrix network 
    client = BeaconWalletClient("My App") {
        support(substrate(), tezos())    
        use(p2pMatrix())
    }

    myCoroutineScope.launch {
      // subscribe to a message Flow
      client.connect().collect { /* process messages */ }
    }
  }
}
```

## Migration

See the below guides to learn how to migrate your existing code to new `Beacon Android SDK` versions.

### From <v3.0.0

As of `v3.0.0`, not only has `Beacon Android SDK` been further split into new modules, it has also become more generic in terms of supported blockchains and transports.
This means that in some parts the values that had been previously set by default now must be configured manually or that various structures have changed their location or definition.
To make sure your existing Beacon integration will be set up the same way as it used to be before `v3.0.0` do the following:

1. Remove the old dependency and add `core`, `client-wallet`, `blockchain-tezos` and `transport-p2p-matrix` modules.

```groovy
def beaconVersion = "3.0.0"

/* <v3.0.0: implementation "com.github.airgap-it:beacon-android-sdk:$beaconVersion" */

implementation "com.github.airgap-it.beacon-android-sdk:core:$beaconVersion"

implementation "com.github.airgap-it.beacon-android-sdk:client-wallet:$beaconVersion"
implementation "com.github.airgap-it.beacon-android-sdk:blockchain-tezos:$beaconVersion"
implementation "com.github.airgap-it.beacon-android-sdk:transport-p2p-matrix:$beaconVersion"
```

2. Replace the old `BeaconClient` with the new `BeaconWalletClient` (`client-wallet`) and configure it with `Tezos` blockchain (`blockchain-tezos`) and `P2pMatrix` transport (`transport-p2p-matrix`).
```kotlin
import it.airgap.beaconsdk.blockchain.tezos.tezos
import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.data.P2P
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix

/* <v3.0.0: val client = BeaconClient("MyApp") */
val client = BeaconWalletClient("MyApp") {
    support(tezos())    
    use(P2P(p2pMatrix())) 
}
```

3. Adjust the message handling code.
```kotlin
/* <v3.0.0:
 * when (beaconRequest) {
 *    is PermissionBeaconRequest -> { ... }
 *    is OperationBeaconRequest -> { ... }
 *    is SignPayloadBeaconRequest -> { ... }
 *    is BroadcastBeaconRequest -> { ... } 
 * }
 */

import it.airgap.beaconsdk.blockchain.tezos.message.request.PermissionTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.BroadcastTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.OperationTezosRequest
import it.airgap.beaconsdk.blockchain.tezos.message.request.SignPayloadTezosRequest

when (beaconRequest) {
    is PermissionTezosRequest -> { /* ... */ }
    is OperationTezosRequest -> { /* ... */ }
    is SignPayloadTezosRequest -> { /* ... */ }
    is BroadcastTezosRequest -> { /* ... */ }
    else -> { /* ... */ }
}
```

```kotlin
/* <v3.0.0:
 * val response = OperationBeaconResponse.from(
 *    operationRequest, //: OperationBeaconRequest 
 *    transactionHash,
 * ) 
 */
import it.airgap.beaconsdk.blockchain.tezos.message.response.OperationTezosResponse
        
val response = OperationTezosResponse.from(
    blockchainBeaconRequest, //: OperationTezosRequest 
    transactionHash,
)
```
```kotlin
/* <v3.0.0:
 * val errorResponse = ErrorBeaconResponse.from(
 *    broadcastRequest, //: BroadcastBeaconRequest 
 *    BeaconError.BroadcastError,
 * ) 
 */

import it.airgap.beaconsdk.blockchain.tezos.data.TezosError

val errorResponse = ErrorBeaconResponse.from(
    broadcastRequest, //: BroadcastTezosRequest
    TezosError.BroadcastError,
)
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
