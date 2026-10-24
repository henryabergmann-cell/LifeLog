import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Billing lives here and only here (PLAN.md §1, D9). The RevenueCat SDK is added as an
// `implementation` dependency in M5, so it never reaches :app's compile classpath.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "app.lifelog.billing"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    api(project(":domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
