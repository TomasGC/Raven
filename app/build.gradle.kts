plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlinx.kover")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "app.raven"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.raven"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "app.raven.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
        // Not the AVD instrumented tests actually run on (that's manually created via avdmanager
        // in Condor's kotlin-instrumented-tests.yml) — this managed-device declaration exists only
        // so `./gradlew pixel4api30Setup` is a real task, letting CI pre-download and cache the
        // system image before creating that manual AVD.
        managedDevices {
            devices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel4api30").apply {
                    device = "Pixel 4"
                    apiLevel = 30
                    systemImageSource = "aosp"
                }
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":placeholder"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    // Overlay windows (TYPE_APPLICATION_OVERLAY) live outside MainActivity's own window, so
    // Espresso (scoped to the app-under-test's window) can't see them. UiAutomator inspects the
    // whole device's window/view hierarchy and can also hold a real touch down for a real
    // duration (UiObject2.click(long)), which real elapsed-time long-press verification needs.
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.12")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.11.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.52")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom("$projectDir/src/main/java")
}

// Test-tier filtering by class-name suffix only, kept deliberately strict and
// consistent from the start: unit tests are plain *Test classes; integration
// tests must be named *IntegrationTest; integration-real tests must be named
// *RealIntegrationTest. Directory placement (unit/, integration-mock/,
// integration-real/) is for human navigation only — this filter, not the
// directory, is what scripts/manage.py's test tiers actually select.
// Tier name matches Condor's kotlin-integration-mock.yml's mock-job
// -DtestType value (Robolectric-shadowed Android runtime = "mock").
afterEvaluate {
    tasks.named("testDebugUnitTest", Test::class.java) {
        val testType = System.getProperty("testType", "all")
        when (testType) {
            "unit" -> filter {
                excludeTestsMatching("*IntegrationTest*")
            }
            "integration-mock" -> filter {
                includeTestsMatching("*IntegrationTest*")
                excludeTestsMatching("*RealIntegrationTest*")
            }
            "integration-real" -> filter {
                includeTestsMatching("*RealIntegrationTest*")
            }
            // "all" -> no filter, runs everything (default for local development)
        }
    }
}

koverReport {
    androidReports("debug") {
        filters {
            excludes {
                classes(
                    "**.R",
                    "**.R$*",
                    "**.BuildConfig",
                    "**.Manifest*",
                    "**.*Test*",
                    "android.*",
                    "androidx.*",
                    "**.*_Hilt*",
                    "**.*_Factory",
                    "**.*_Factory$*",
                    "**.*_MembersInjector",
                    "**.Hilt_*",
                    "dagger.hilt.internal.aggregatedroot.codegen.*",
                    "hilt_aggregated_deps.*",
                    "app.raven.RavenApplication",
                    "app.raven.MainActivity",
                    "app.raven.MainActivity$*",
                    "app.raven.service.OverlayService",
                    "app.raven.service.OverlayService$*",
                    "app.raven.service.OverlayWindowController",
                    "app.raven.service.OverlayWindowController$*",
                    "app.raven.ui.screen.*",
                    "app.raven.ui.theme.*",
                    "app.raven.di.AppModule",
                    "app.raven.di.AppModule_*"
                )
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
