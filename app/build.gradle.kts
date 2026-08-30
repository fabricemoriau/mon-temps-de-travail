import com.google.firebase.appdistribution.gradle.firebaseAppDistribution

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.example.montempsdetravail"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.montempsdetravail"
        minSdk = 24
        targetSdk = 35
        versionCode = 15
        versionName = "1.14"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            firebaseAppDistribution {
                appId = "1:378775600000:android:be03a2e059f9396e665f65"
                groups = "collegues"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            firebaseAppDistribution {
                appId = "1:378775600000:android:be03a2e059f9396e665f65"
                groups = "collegues"
            }
        }
    }

    applicationVariants.all {
        val variant = this
        val customFileName = "MonTemps_v1.14.apk"
        
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = customFileName
        }

        // Créer un dossier "apk final" à la racine et copier l'APK après le build
        variant.assembleProvider.configure {
            doLast {
                val outputDir = File(project.rootDir, "apk final")
                if (!outputDir.exists()) outputDir.mkdirs()
                
                variant.outputs.forEach { output ->
                    val apkFile = output.outputFile
                    if (apkFile.exists()) {
                        apkFile.copyTo(File(outputDir, customFileName), overwrite = true)
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.appcompat)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("org.jetbrains.compose.runtime:runtime:1.7.0")
    implementation("org.jetbrains.compose.ui:ui:1.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database.ktx)
    
    // Architecture
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.work.runtime.ktx)
    
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // UI Libraries
    implementation(libs.glide)
    implementation(libs.play.services.location)
    implementation(libs.mp.android.chart)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
