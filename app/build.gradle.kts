import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

apply(plugin = "stringfog")

android {
    namespace = "com.scottyab.rootbeer.sample"

    defaultConfig {
        applicationId = "com.scottyab.rootbeer.sample"
        versionName = version.toString()
        versionCode = findProperty("VERSION_CODE").toString().toInt()
        vectorDrawables.useSupportLibrary = true

        base.archivesName = "RootBeerSample-$versionName-[$versionCode]"
    }

    buildFeatures {
        viewBinding = true
    }

    // check if the keystore details are defined in gradle.properties (this is so the key is not in github)
    if (findProperty("ROOTBEER_SAMPLE_STORE") != null) {
        signingConfigs {
            // from ~/user/.gradle/gradle.properties
            create("release") {
                storeFile = file(findProperty("ROOTBEER_SAMPLE_STORE").toString())
                keyAlias = findProperty("ROOTBEER_SAMPLE_KEY").toString()
                storePassword = findProperty("ROOTBEER_SAMPLE_STORE_PASS").toString()
                keyPassword = findProperty("ROOTBEER_SAMPLE_KEY_PASS").toString()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }

        release {
            if (rootProject.hasProperty("ROOTBEER_SAMPLE_STORE")) {
                signingConfig = signingConfigs["release"]
            }

            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":rootbeerlib"))
    // used when testing the snapshot lib
    // implementation("com.scottyab:rootbeer-lib:0.1.1-SNAPSHOT")
    implementation("com.github.megatronking.stringfog:xor:5.0.0-llm3")

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.android)

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.appcompat)
    implementation(libs.android.google.material)

    implementation(libs.nineoldandroids)
    implementation(libs.beerprogressview)

    implementation(libs.timber)
}

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    enable = true
    fogPackages = arrayOf("com.scottyab.rootbeer.sample")
    kg = RandomKeyGenerator()
    mode = StringFogMode.visible
    visiblePrefix = "商业软件禁止逆向"
    fogClassPackage = "o0O"
    fogClassName = "O00"
    generateFogClass = true
}
