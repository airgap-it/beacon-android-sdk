object Android {
    const val compileSdk = 30
    const val minSdk = 21
    const val targetSdk = 30

    const val versionCode = 15
    const val versionName = "3.1.0"
}

object Version {
    const val kotlin = "1.5.30"

    const val kotlinSerialization = "1.3.1"

    const val androidxCore = "1.6.0"
    const val androidxAppCompat = "1.3.1"
    const val androidxConstraintLayout = "2.1.0"

    const val androidxActivity = "1.3.1"
    const val androidxLifecycle = "2.3.1"

    const val androidxSecurity = "1.0.0"

    const val coroutines = "1.5.1"

    const val ktor = "1.6.2"

    const val lazySodium = "5.0.2"
    const val jna = "5.9.0"

    const val materialComponents = "1.4.0"

    const val junit = "4.13.2"

    const val androidxJunit = "1.1.3"
    const val androidxEspresso = "3.4.0"

    const val mockk = "1.12.0"

    const val bouncyCastle = "1.70"
}

object Dependencies {
    const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
    const val kotlinReflection = "org.jetbrains.kotlin:kotlin-reflect:${Version.kotlin}"

    const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinSerialization}"
    const val kotlinxCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.coroutines}"
    const val kotlinxCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"

    const val ktorOkHttp = "io.ktor:ktor-client-okhttp:${Version.ktor}"
    const val ktorJson = "io.ktor:ktor-client-json:${Version.ktor}"
    const val ktorSerializationJvm = "io.ktor:ktor-client-serialization-jvm:${Version.ktor}"
    const val ktorLoggingJvm = "io.ktor:ktor-client-logging-jvm:${Version.ktor}"

    const val androidxCore = "androidx.core:core-ktx:${Version.androidxCore}"

    const val androidxAppCompat = "androidx.appcompat:appcompat:${Version.androidxAppCompat}"
    const val androidxConstraintLayout = "androidx.constraintlayout:constraintlayout:${Version.androidxConstraintLayout}"

    const val androidxActivity = "androidx.activity:activity-ktx:${Version.androidxActivity}"

    const val androidxLifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Version.androidxLifecycle}"
    const val androidxLifecycleLiveData = "androidx.lifecycle:lifecycle-livedata-ktx:${Version.androidxLifecycle}"
    const val androidxLifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${Version.androidxLifecycle}"

    const val androidxSecurity = "androidx.security:security-crypto:${Version.androidxSecurity}"

    const val materialComponents = "com.google.android.material:material:${Version.materialComponents}"

    const val lazySodium = "com.goterl:lazysodium-android:${Version.lazySodium}@aar"
    const val jna = "net.java.dev.jna:jna:${Version.jna}@aar"
}

object TestDependencies {
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Version.kotlin}"
    const val kotlinxCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Version.coroutines}"

    const val junit = "junit:junit:${Version.junit}"

    const val androidxJunit = "androidx.test.ext:junit:${Version.androidxJunit}"
    const val androidxEspresso = "androidx.test.espresso:espresso-core:${Version.androidxEspresso}"

    const val mockk = "io.mockk:mockk:${Version.mockk}"

    const val bouncyCastle = "org.bouncycastle:bcprov-jdk15on:${Version.bouncyCastle}"
}