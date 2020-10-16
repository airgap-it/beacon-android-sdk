object Version {
    const val kotlin = "1.4.10"

    const val kotlinSerialization = "1.0.0-RC2"

    const val androidxCore = "1.3.1"
    const val androidxAppCompat = "1.2.0"
    const val androidxConstraintLayout = "1.1.3"

    const val coroutines = "1.4.0-M1"
    const val lazySodium = "4.2.0"
    const val jna = "5.6.0"

    const val junit = "4.13"

    const val androidxJunit = "1.1.2"
    const val androidxEspresso = "3.3.0"

    const val mockk = "1.10.2"
}

object Dependencies {
    const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
    const val kotlinReflection = "org.jetbrains.kotlin:kotlin-reflect:${Version.kotlin}"

    const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinSerialization}"
    const val kotlinxCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.coroutines}"
    const val kotlinxCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"

    const val androidxCore = "androidx.core:core-ktx:${Version.androidxCore}"
    const val androidxAppCompat = "androidx.appcompat:appcompat:${Version.androidxAppCompat}"
    const val androidxConstraintLayout = "androidx.constraintlayout:constraintlayout:${Version.androidxConstraintLayout}"

    const val lazySodium = "com.goterl.lazycode:lazysodium-android:${Version.lazySodium}@aar"
    const val jna = "net.java.dev.jna:jna:${Version.jna}@aar"
}

object TestDependencies {
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Version.kotlin}"
    const val kotlinxCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Version.coroutines}"

    const val junit = "junit:junit:${Version.junit}"

    const val androidxJunit = "androidx.test.ext:junit:${Version.androidxJunit}"
    const val androidxEspresso = "androidx.test.espresso:espresso-core:${Version.androidxEspresso}"

    const val mockk = "io.mockk:mockk:${Version.mockk}"
}