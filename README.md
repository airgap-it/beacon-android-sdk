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
      // ...
      maven { url 'https://jitpack.io' }
    }
  }
  ```

  2. Add the dependency:
  ```groovy
  def beaconVersion = "3.0.0"

  implementation "com.github.airgap-it.beacon-android-sdk:core:$beaconVersion" // core, **required** 

  implementation "com.github.airgap-it.beacon-android-sdk:client-wallet:$beaconVersion" // client-wallet, optional 
  implementation "com.github.airgap-it.beacon-android-sdk:client-wallet-compat:$beaconVersion" // client-wallet-compat, optional 
  implementation "com.github.airgap-it.beacon-android-sdk:blockchain-tezos:$beaconVersion" // blockchain-tezos, optional 
  implementation "com.github.airgap-it.beacon-android-sdk:transport-p2p-matrix:$beaconVersion" // blockchain-tezos, optional 
  
  ---
  implementation "com.github.airgap-it:beacon-android-sdk:$beaconVersion" // alternatively, all modules 
  ```

### Troubleshooting

See the list of known issues and how to fix them if you run into problems after adding the dependencies.

- `Native library (com/sun/jna/xxxxx/libjnidispatch.so) not found in resource path`

    Add the `"net.java.dev.jna:jna:x.y.z@aar"` dependency **and exclude the `net.java.dev.jna` group from the Beacon dependencies**.
    ```groovy
    def withoutJna = { exclude group: 'net.java.dev.jna' }
    
    implementation "com.github.airgap-it.beacon-android-sdk:core:$beaconVersion", withoutJna
    implementation "com.github.airgap-it.beacon-android-sdk:client-wallet:$beaconVersion", withoutJna 
    // ...

    implementation "net.java.dev.jna:jna:5.9.0@aar"
    ```

<!-- TODO: ## Documentation -->

## Project Overview

The project consists of the following modules:

- `core` - common and base code for other modules
- `client-wallet` - the wallet implementation of Beacon
- `client-wallet-compat` - a supplementary interface for `client-wallet` for use without [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- `blockchain-tezos` - a set of messages, utility functions and other components specific for Tezos
- `transport-p2p-matrix` - Beacon P2P implementation which uses [Matrix](https://matrix.org/) network for the communication
- `demo` - an example application

## Examples

The snippets below show how to quickly setup listening for incoming Beacon messages in Kotlin with coroutines. 

For more examples or examples of how to use the SDK without coroutines or in Java, please see our `demo` app (WIP).

### Create a Beacon wallet client and listen for incoming requests

```kotlin
import it.airgap.beaconsdk.blockchain.tezos.tezos
import it.airgap.beaconsdk.client.wallet.BeaconWalletClient
import it.airgap.beaconsdk.core.data.P2P
import it.airgap.beaconsdk.transport.p2p.matrix.p2pMatrix

class MainActivity : AppCompatActivity() {
  lateinit var client: BeaconWalletClient

  // ...

  suspend fun listenForBeaconMessages() {
    // create a wallet Beacon client that can listen for Tezos messages via Matrix network 
    client = BeaconWalletClient("My App", listOf(tezos())) { 
        addConnections(P2P(p2pMatrix()))
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
val client = BeaconWalletClient("MyApp", listOf(tezos())) { 
    addConnections(P2P(p2pMatrix())) 
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
