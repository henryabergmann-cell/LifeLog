import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---------------------------------------------------------------------------------------------
// Build configuration comes from local.properties (git-ignored) or environment variables.
// Product IDs, keys and URLs are never hard-coded in source. See local.properties.example.
// ---------------------------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun configValue(key: String): String? =
    localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.environmentVariable(key.uppercase().replace('.', '_')).orNull?.trim()?.takeIf { it.isNotEmpty() }

fun requireConfig(key: String): String = checkNotNull(configValue(key)) { "Missing $key" }

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val requiredConfigKeys = listOf(
    "lifelog.billing.productId",
    "lifelog.billing.basePlan.monthly",
    "lifelog.billing.basePlan.yearly",
    "lifelog.billing.offer.yearlyTrial",
    "lifelog.url.privacy",
    "lifelog.url.terms",
)
val signingConfigKeys = listOf(
    "lifelog.signing.storeFile",
    "lifelog.signing.storePassword",
    "lifelog.signing.keyAlias",
    "lifelog.signing.keyPassword",
)

requiredConfigKeys.filter { configValue(it) == null }.takeIf { it.isNotEmpty() }?.let { missing ->
    throw GradleException(
        "Missing build configuration: ${missing.joinToString()}.\n" +
            "Copy local.properties.example to local.properties and fill in the values " +
            "(or set the matching environment variables, e.g. LIFELOG_BILLING_PRODUCTID).",
    )
}

android {
    namespace = "app.lifelog"
    // compileSdk 37 is required by current AndroidX releases. Runtime behaviour follows targetSdk.
    compileSdk = 37

    defaultConfig {
        applicationId = providers.gradleProperty("lifelog.applicationId").get()
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BILLING_PRODUCT_ID", requireConfig("lifelog.billing.productId").asBuildConfigString())
        buildConfigField("String", "BILLING_BASE_PLAN_MONTHLY", requireConfig("lifelog.billing.basePlan.monthly").asBuildConfigString())
        buildConfigField("String", "BILLING_BASE_PLAN_YEARLY", requireConfig("lifelog.billing.basePlan.yearly").asBuildConfigString())
        buildConfigField("String", "BILLING_OFFER_YEARLY_TRIAL", requireConfig("lifelog.billing.offer.yearlyTrial").asBuildConfigString())
        buildConfigField("String", "BILLING_LICENSE_KEY", (configValue("lifelog.billing.licenseKey") ?: "").asBuildConfigString())
        buildConfigField("String", "PRIVACY_POLICY_URL", requireConfig("lifelog.url.privacy").asBuildConfigString())
        buildConfigField("String", "TERMS_URL", requireConfig("lifelog.url.terms").asBuildConfigString())
    }

    signingConfigs {
        if (signingConfigKeys.all { configValue(it) != null }) {
            create("release") {
                storeFile = rootProject.file(requireConfig("lifelog.signing.storeFile"))
                storePassword = requireConfig("lifelog.signing.storePassword")
                keyAlias = requireConfig("lifelog.signing.keyAlias")
                keyPassword = requireConfig("lifelog.signing.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// Release packaging needs signing keys, the license key and real legal URLs. Debug builds and
// unit tests don't, so this check is attached to release packaging tasks only.
val missingReleaseConfig: List<String> =
    (signingConfigKeys + "lifelog.billing.licenseKey").filter { configValue(it) == null } +
        listOf("lifelog.url.privacy", "lifelog.url.terms")
            .filter { configValue(it).orEmpty().contains("example.com") }
            .map { "$it (placeholder URL)" }

val validateReleaseConfig = tasks.register("validateReleaseConfig") {
    group = "verification"
    description = "Fails release packaging when release-only configuration is missing."
    val missing = missingReleaseConfig
    doLast {
        if (missing.isNotEmpty()) {
            throw GradleException("Release build is missing configuration: ${missing.joinToString()}. See local.properties.example.")
        }
    }
}

// Developer options and the fake-Premium switch live in the debug source set only. This guards
// against them reaching the store build if that code is ever moved to main.
val verifyReleaseHasNoDebugCode = tasks.register("verifyReleaseHasNoDebugCode") {
    group = "verification"
    description = "Fails release packaging when debug-only classes are in the release build."
    val mapping = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
    dependsOn("minifyReleaseWithR8")
    inputs.file(mapping)
    doLast {
        val forbidden = listOf("DebugPremiumOverride", "DebugPremiumModule", "DeveloperOptionsViewModel")
        val found = mapping.get().asFile.readLines()
            .filter { line -> !line.startsWith(" ") && forbidden.any { line.contains(it) } }
        if (found.isNotEmpty()) {
            throw GradleException("Debug-only code in the release build:\n" + found.joinToString("\n"))
        }
    }
}

tasks.named { it in setOf("packageRelease", "packageReleaseBundle", "assembleRelease", "bundleRelease") }
    .configureEach { dependsOn(validateReleaseConfig, verifyReleaseHasNoDebugCode) }

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":billing"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    // Only the debug "simulate Premium" toggle stores data from :app directly.
    debugImplementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
