import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

val deadlinerCoreSyncMode = providers
    .gradleProperty("deadliner.core.sync.mode")
    .orElse(
        providers.environmentVariable("DEADLINER_CORE_SOURCE")
            .orElse("release")
    )
val deadlinerCoreSyncEnabled = providers
    .gradleProperty("deadliner.core.sync.enabled")
    .orElse("true")
    .map { it.equals("true", ignoreCase = true) }
val deadlinerCoreGithubToken = providers
    .gradleProperty("deadliner.core.github.token")
    .orElse(
        providers.environmentVariable("DEADLINER_CORE_GITHUB_TOKEN")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
    )

val syncDeadlinerCoreAndroid by tasks.registering(Exec::class) {
    group = "deadliner"
    description = "Sync Android JNI libs and Kotlin bindings for Deadliner core."

    val syncScript = rootProject.file("scripts/sync_deadliner_core_android.sh")
    commandLine("bash", syncScript.absolutePath, deadlinerCoreSyncMode.get())
    environment(
        "DEADLINER_CORE_GITHUB_TOKEN",
        deadlinerCoreGithubToken.orNull ?: ""
    )

    inputs.file(syncScript)
    outputs.dir(rootProject.file(".deadliner-core/android"))
    outputs.upToDateWhen { false }
    onlyIf { deadlinerCoreSyncEnabled.get() }
}

android {
    namespace = "com.aritxonly.deadliner"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aritxonly.deadliner"
        minSdk = 31
        targetSdk = 36
        versionCode = 33
        versionName = "5.0.0-Beta1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$timeStamp\"")

        val secret: String =
            env.fetchOrNull("DEADLINER_APP_SECRET")              // .env
                ?: System.getenv("DEADLINER_APP_SECRET")           // CI 环境变量兜底
                ?: System.getProperty("DEADLINER_APP_SECRET")      // -D 注入兜底
                ?: ""
        buildConfigField("String", "DEADLINER_APP_SECRET", "\"$secret\"")
    }
    lint {
        disable += "NullSafeMutableLiveData"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("main") {
            java.srcDir(rootProject.file(".deadliner-core/android/bindings"))
            jniLibs.srcDir(rootProject.file(".deadliner-core/android/jniLibs"))
        }
    }
    ndkVersion = "26.1.10909125"
}

tasks.named("preBuild") {
    dependsOn(syncDeadlinerCoreAndroid)
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
    implementation("nl.dionsegijn:konfetti-compose:2.0.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0-beta02")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-beta02")
    implementation("io.github.ehsannarmani:compose-charts:0.1.7")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("com.github.jeziellago:compose-markdown:0.5.7")
    implementation("androidx.window:window:1.4.0")
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation("com.airbnb.android:lottie:6.6.6")
    implementation("androidx.databinding:viewbinding:8.10.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.1")
    implementation("androidx.datastore:datastore-preferences-android:1.1.7")
    implementation("io.github.rroohit:ImageCropView:3.1.1")
    implementation("com.materialkolor:material-kolor:4.1.1")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.animation.tooling.internal)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.animation)
    implementation(libs.androidx.ui.viewbinding)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
