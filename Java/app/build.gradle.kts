plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "es.ifp.fairpay"
    compileSdk = 35

    defaultConfig {
        applicationId = "es.ifp.fairpay"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // --- INTEGRACIÓN BLOCKCHAIN & SEGURIDAD ---
    implementation("org.web3j:core:4.8.8-android")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation ("mysql:mysql-connector-java:5.1.49")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- DISEÑO Y NAVEGACIÓN ---

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // AndroidX Core
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.9.2")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation(libs.biometric)

    // --- TESTING ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}