// build.gradle (Module :app) - NENHUMA ALTERAÇÃO NECESSÁRIA
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.intro.leitormendolo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.intro.leitormendolo"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Declarações explícitas para evitar qualquer conflito
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0") // Já presente
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.media:media:1.7.0")

    // Dependências do nosso projeto
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // A nossa dependência local, declarada da forma mais simples
    implementation(files("libs/acrcloud-universal-sdk-1.3.30.jar"))

    // Dependências de teste
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
}