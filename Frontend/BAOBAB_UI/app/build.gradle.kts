import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseApiUrl = providers.gradleProperty("releaseApiBaseUrl").orNull
val validateReleaseApiUrl = tasks.register("validateReleaseApiUrl") {
    inputs.property("apiUrl", releaseApiUrl.orEmpty())
    doLast {
        val uri = runCatching { URI(inputs.properties["apiUrl"].toString()) }.getOrNull()
        require(uri?.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.query == null && uri.fragment == null && uri.host !in listOf("localhost", "10.0.2.2", "127.0.0.1", "configure.invalid")) {
            "릴리스 빌드에는 -PreleaseApiBaseUrl=https://<배포 서버>/api/ 설정이 필요합니다."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseApiUrl) }

android {
    namespace = "com.example.baobab"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.baobab"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        val apiBaseUrl = providers.gradleProperty("surveyApiBaseUrl")
            .getOrElse("http://10.0.2.2:5000/api/")
        buildConfigField("String", "SURVEY_API_BASE_URL", "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            val endpoint = releaseApiUrl ?: "https://configure.invalid/api/"
            buildConfigField("String", "SURVEY_API_BASE_URL", "\"${endpoint.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation("androidx.compose.material:material-icons-extended")
}
