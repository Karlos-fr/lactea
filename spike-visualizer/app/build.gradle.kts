plugins {
    id("com.android.application")
}

android {
    namespace = "fr.milkfy.spike"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.milkfy.spike"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-spike"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
