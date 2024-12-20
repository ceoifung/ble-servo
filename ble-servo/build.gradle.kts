plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("org.jetbrains.dokka")
}
android {
    namespace = "com.xiaor.libservo"
    compileSdk = 33

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("com.github.Jasonchenlijian:FastBle:2.4.0"){
        isTransitive = true
    }

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.xiaor.libservo"
            artifactId = "lib-ble-servo"
            version = "1.0.0"
            afterEvaluate {
//                from(components["release"])
                artifact(tasks.getByName("bundleReleaseAar"))
//                pom.withXml {
//                    val dependenciesNode = asNode().appendNode("dependencies")
//                    configurations.api.get().allDependencies.forEach { dep ->
//                        if (dep.group != null && dep.version != null) {
//                            val dependencyNode = dependenciesNode.appendNode("dependency")
//                            println(dep.group + ":" + dep.name + ":" + dep.version)
//                            dependencyNode.appendNode("groupId", dep.group)
//                            dependencyNode.appendNode("artifactId", dep.name)
//                            dependencyNode.appendNode("version", dep.version)
//                        }
//                    }
//                }
            }
        }
    }
}
