plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.harshkanjariya.autohome"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.harshkanjariya.autohome"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.0.148:3000\"")
            buildConfigField("String", "MQTT_URL", "\"ssl://5b960086f3b74b0d965b7532908b1914.s1.eu.hivemq.cloud:8883\"")
            buildConfigField("String", "MQTT_USERNAME", "\"hivemq.webclient.1728037900518\"")
            buildConfigField("String", "MQTT_PASSWORD", "\"hBH0Kar@b>;9x21Zg:AR\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.auto-home.in\"")
            buildConfigField("String", "MQTT_URL", "\"ssl://5b960086f3b74b0d965b7532908b1914.s1.eu.hivemq.cloud:8883\"")
            buildConfigField("String", "MQTT_USERNAME", "\"hivemq.webclient.1728037900518\"")
            buildConfigField("String", "MQTT_PASSWORD", "\"hBH0Kar@b>;9x21Zg:AR\"")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,io.netty.versions.properties}"
        }
    }
}

val plutoVersion = "2.2.2"

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.googleid)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Dagger Hilt dependencies
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")


    debugImplementation("com.plutolib:pluto:$plutoVersion")
    debugImplementation("com.plutolib.plugins:network:$plutoVersion")
    debugImplementation("com.plutolib.plugins:network-interceptor-okhttp:$plutoVersion")
    releaseImplementation("com.plutolib.plugins:network-interceptor-ktor-no-op:$plutoVersion")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.27.0")
    implementation("javax.jmdns:jmdns:3.4.1")
    implementation("com.google.accompanist:accompanist-flowlayout:0.30.0")
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.github.hannesa2:paho.mqtt.android:4.2.4")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    implementation("com.google.android.gms:play-services-auth:20.2.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.auth0:java-jwt:4.2.0")
}
