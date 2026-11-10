plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

import java.util.Properties

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

val releaseSigningEnabled = (findProperty("ENABLE_RELEASE_SIGNING") as String? ?: "false").toBooleanStrict()
val deepLinkHost = (findProperty("DEEP_LINK_HOST") as String? ?: "example.invalid")
require(deepLinkHost.matches(Regex("[A-Za-z0-9.-]+"))) { "Invalid DEEP_LINK_HOST" }
fun signingValue(property: String, environment: String): String =
    (keystoreProperties.getProperty(property) ?: System.getenv(environment))
        ?.takeIf { it.isNotBlank() } ?: error("Release signing requires $environment or $property")

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.media3.common.util.UnstableApi")
    }
}

// === KTLINT — Kotlin style enforcement ===
// Pre-commit: only check main source set.
// Test files are formatted with ktlintFormat as a separate step.
ktlint {
    android = true
    ignoreFailures = true
    disabledRules.set(
        setOf("function-naming"),
    )
    filter {
        include("**/src/main/**/*.kt")
        exclude("**/test/**")
        exclude("**/*.kts")
    }
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

// Disable ktlint check for test source sets — not enforced in pre-commit.
// Test files use loose formatting conventions; fix with: ./gradlew ktlintFormat
tasks.matching { it.name == "ktlintTestSourceSetCheck" }.configureEach { enabled = false }
tasks.matching { it.name == "ktlintTestSourceSetFormat" }.configureEach { enabled = false }
tasks.matching { it.name == "ktlintTestDebugSourceSetCheck" }.configureEach { enabled = false }
tasks.matching { it.name == "ktlintTestFixturesSourceSetCheck" }.configureEach { enabled = false }
tasks.matching { it.name == "ktlintKotlinScriptCheck" }.configureEach { enabled = false }

android {
    namespace = "com.dominickp.firetvandroidtv"
    compileSdk = libs.versions.compileSdk.get().toInt()
    (findProperty("APP_ASSETS_DIR") as String?)?.let { assetsDir ->
        require(rootProject.file("$assetsDir/app_config.json").isFile) { "APP_ASSETS_DIR must contain app_config.json" }
        sourceSets.getByName("main").assets.setSrcDirs(listOf(rootProject.file(assetsDir)))
    }

    defaultConfig {
        applicationId = (findProperty("APPLICATION_ID") as String? ?: "com.example.tv")
        manifestPlaceholders["deepLinkHost"] = deepLinkHost
        buildConfigField("String", "DEEP_LINK_HOST", "\"$deepLinkHost\"")
        minSdk = 25
        targetSdk = libs.versions.targetSdk.get().toInt()
        // Read versionCode from VERSION_CODE env var or Gradle property (default 1).
        // CI sets VERSION_CODE via git rev-list --count; local builds can pass -PVERSION_CODE=N.
        versionCode = (System.getenv("VERSION_CODE")
            ?: (project.findProperty("VERSION_CODE") as String?)
            ?: "1").toInt()
        versionName = (System.getenv("VERSION_NAME")
            ?: (project.findProperty("VERSION_NAME") as String?)
            ?: "1.0.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Increase test runner startup timeout for slow emulator cold-start
        testInstrumentationRunnerArguments["timeout"] = "120000"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (releaseSigningEnabled) {
            create("release") {
                storeFile = rootProject.file(signingValue("keystore.file", "KEYSTORE_FILE"))
                require(storeFile!!.isFile) { "Release keystore file does not exist" }
                storePassword = signingValue("keystore.password", "KEYSTORE_PASSWORD")
                keyAlias = signingValue("keystore.alias", "KEY_ALIAS")
                keyPassword = signingValue("key.password", "KEY_PASSWORD")
            }
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningEnabled) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Suppress OptIn usage errors for Media3 UnstableApi — globally opted-in via compiler args
        lintConfig = file("lint-xml-config.xml")
    }
}

dependencies {
    // --- Core Android ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // --- Compose (BOM) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // --- Leanback (TV-specific UI: RowList, Presenter, etc.) ---
    implementation(libs.androidx.leanback)

    // --- Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Lifecycle & ViewModel ---
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.android)

    // --- Serialization (parse JSON config + feed) ---
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core)
    // --- Data (Room + DataStore) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // --- Networking (Retrofit + OkHttp for feed API + analytics) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // --- Image Loading (Coil — loads poster/thumbnail URLs) ---
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // --- Compose Icons (icons beyond basic set — Pause, Replay, Forward) ---
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.animation)

    // --- Media (ExoPlayer / Media3 for video playback) ---
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)

    // --- Testing ---
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.vintage)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
    // Force test coroutines version to 1.6.4 so the whole test classpath is
    // consistent. Core runs coroutines 1.9.0 and Turbine 1.2.0 depends on
    // coroutines-test 1.9.0; without this pin Gradle would resolve the test
    // classpath to 1.9.0, where StandardTestDispatcher is a *class* rather
    // than the 1.6.4 *top-level factory function* the migrated tests rely on.
    // The catalog `coroutinesTest` version and this strategy stay in sync.
    configurations.configureEach {
        if (name.toLowerCase().contains("test")) {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlinx" && requested.name.contains("coroutines-test")) {
                    useVersion(libs.versions.coroutinesTest.get())
                    because(
                        "Align test classpath on coroutines-test 1.6.4: 1.9.0 changed " +
                            "StandardTestDispatcher from a factory function to a class, " +
                            "which would break the migrated test dispatchers. " +
                            "(Turbine 1.2.0 pulls coroutines-test 1.9.0 by default.)",
                    )
                }
            }
        }
    }
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.manifest)
    // Orchestrator for headless emulator support
    androidTestUtil(libs.test.orchestrator)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// === JUnit 5 + Robolectric + Vintage + Turbine test task configuration ===
tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "junit-vintage")
    }
    // ContinueWatchingDaoTest uses Robolectric Room which needs Android SDK artifacts
    // that aren't available on all hosts — skip on JVM, run via instrumented tests instead
    exclude("com/dominickp/firetvandroidtv/dao/ContinueWatchingDaoTest.class")
    systemProperty("robolectric.pixelCopyStrategy", " native")
    // Run test classes in parallel JVM processes (not threads).
    // Safe: each fork is an isolated process, so global state like
    // Dispatchers.setMain() doesn't leak across forks.
    // Halve available CPUs to leave headroom for Gradle daemon + compile workers.
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// === Build verification task — runs full test + lint cycle ===
tasks.register("verifyBuild") {
    description = "Run clean, build, test, ktlint, and lint in one step"
    dependsOn("clean", "testDebugUnitTest", "ktlintCheck", "lintDebug")
}
