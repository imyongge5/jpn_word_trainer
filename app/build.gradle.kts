import java.io.ByteArrayOutputStream
import java.util.Properties
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("com.google.firebase.appdistribution")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

fun runGitCommand(vararg args: String): String? {
    val output = ByteArrayOutputStream()
    val errorOutput = ByteArrayOutputStream()
    return try {
        exec {
            commandLine("git", *args)
            standardOutput = output
            this.errorOutput = errorOutput
            isIgnoreExitValue = true
        }
        output.toString().trim().ifBlank { null }
    } catch (_: Exception) {
        null
    }
}

fun normalizeVersionTag(rawTag: String?): String? {
    if (rawTag.isNullOrBlank()) {
        return null
    }
    return when {
        rawTag.startsWith("build-v") -> rawTag.removePrefix("build-")
        rawTag.startsWith("v") -> rawTag
        else -> null
    }
}

fun computeVersionFromHistory(versionA: Int, versionB: Int): String {
    val baseCommit = runGitCommand("log", "-n", "1", "--format=%H", "--", "version-series.properties")
        ?: return "v$versionA.$versionB.1"
    val commitsAfterBase = runGitCommand("rev-list", "--count", "$baseCommit..HEAD")
        ?.toIntOrNull()
        ?: 0
    val versionCcc = commitsAfterBase + 1
    return "v$versionA.$versionB.$versionCcc"
}

val versionSeries = Properties().apply {
    rootProject.file("version-series.properties").inputStream().use(::load)
}

val versionA = versionSeries.getProperty("VERSION_A").toInt()
val versionB = versionSeries.getProperty("VERSION_B").toInt()

val exactHeadTag = runGitCommand("tag", "--points-at", "HEAD")
    ?.lineSequence()
    ?.map(String::trim)
    ?.map(::normalizeVersionTag)
    ?.firstOrNull { it != null }

val envVersionTag = normalizeVersionTag(providers.environmentVariable("APP_VERSION_TAG").orNull)

val resolvedVersionTag = envVersionTag
    ?: exactHeadTag
    ?: computeVersionFromHistory(versionA, versionB)

val versionMatch = Regex("""^v(\d+)\.(\d+)\.(\d+)$""").matchEntire(resolvedVersionTag)
    ?: throw GradleException("유효한 버전 태그 형식이 아닙니다: $resolvedVersionTag")

val appVersionName = resolvedVersionTag
val appVersionCode = versionMatch.groupValues[1].toInt() * 100_000 +
    versionMatch.groupValues[2].toInt() * 1_000 +
    versionMatch.groupValues[3].toInt()
val firebaseAndroidAppId = "1:1052747644476:android:0ea792ded8fa02bcd50d39"
val firebaseServiceCredentialsFile = providers.gradleProperty("firebaseServiceCredentialsFile").orNull
    ?: providers.environmentVariable("GOOGLE_APPLICATION_CREDENTIALS").orNull
val firebaseArtifactPath = providers.gradleProperty("firebaseArtifactPath").orNull
    ?: providers.environmentVariable("FIREBASE_DISTRIBUTION_ARTIFACT_PATH").orNull

android {
    namespace = "com.mistbottle.jpnwordtrainer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mistbottle.jpnwordtrainer"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            firebaseAppDistribution {
                appId = firebaseAndroidAppId
                firebaseServiceCredentialsFile?.let {
                    serviceCredentialsFile = it
                }
                firebaseArtifactPath?.let {
                    artifactPath = it
                }
                artifactType = "APK"
                providers.environmentVariable("FIREBASE_APP_DISTRIBUTION_GROUPS").orNull?.let {
                    groups = it
                }
                providers.environmentVariable("FIREBASE_APP_DISTRIBUTION_TESTERS").orNull?.let {
                    testers = it
                }
                providers.environmentVariable("FIREBASE_RELEASE_NOTES_FILE").orNull?.let {
                    releaseNotesFile = it
                }
            }
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            firebaseAppDistribution {
                appId = firebaseAndroidAppId
                firebaseServiceCredentialsFile?.let {
                    serviceCredentialsFile = it
                }
                firebaseArtifactPath?.let {
                    artifactPath = it
                }
                artifactType = "APK"
                providers.environmentVariable("FIREBASE_APP_DISTRIBUTION_GROUPS").orNull?.let {
                    groups = it
                }
                providers.environmentVariable("FIREBASE_APP_DISTRIBUTION_TESTERS").orNull?.let {
                    testers = it
                }
                providers.environmentVariable("FIREBASE_RELEASE_NOTES_FILE").orNull?.let {
                    releaseNotesFile = it
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.10.01")
    val roomVersion = "2.7.2"

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.navigation:navigation-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.google.android.material:material:1.13.0")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(bom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
