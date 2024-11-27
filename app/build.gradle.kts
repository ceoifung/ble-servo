import org.jetbrains.kotlin.konan.properties.Properties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val versionCodeProp by extra { project.getProperty("version.properties", "versionCode") as Int }
val versionNameProp by extra { project.getProperty("version.properties", "versionName") as String }

android {
    namespace = "com.xiaor.ble"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xiaor.ble"
        minSdk = 24
        targetSdk = 34
        versionCode = versionCodeProp
        versionName = versionNameProp

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
    kotlinOptions {
        jvmTarget = "1.8"
    }

    applicationVariants.all {
        // 这里会为每个构建变体设置输出文件名
        outputs.all {
            // 判断是否是 APK 输出类型
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                // 设置 APK 的输出文件名
                // 可以使用构建类型（buildType.name）和版本名（defaultConfig.versionName）来定义文件名
                outputFileName = "BleServo-${defaultConfig.versionName}-${buildType.name}.apk"
            }
        }
    }

}

// 获取软件的版本信息
fun Project.getProperty(filename: String, propName: String): Any? {
    val versionFile = File(filename)
    return if (versionFile.canRead()) {
        val props = Properties()
        props.load(FileInputStream(versionFile))

        when (propName) {
            "versionCode" -> {
                val versionCode = props.getProperty(propName)?.toIntOrNull()
                versionCode?.let {
                    val runTasks = gradle.startParameter.taskNames
                    runTasks.forEach { taskName ->
                        if (taskName.contains("assembleRelease")) {
                            val newVersionCode = it + 1
                            props[propName] = newVersionCode.toString()
                            FileOutputStream(versionFile).use { fos ->
                                props.store(fos, null)
                            }
                        }
                    }
                } ?: return null
                versionCode
            }
            "versionName" -> {
                val versionName = props.getProperty(propName)
                val versionCode = props.getProperty("versionCode")
                val today = SimpleDateFormat("yyMMdd").format(Date())
                val process = Runtime.getRuntime().exec("git rev-parse --short HEAD")
                process.waitFor()
                val sha1 = process.inputStream.bufferedReader().use { it.readText().trim() }
                val resName = "$versionName.$versionCode.$today.$sha1"
                resName
            }
            else -> null
        }
    } else {
        println("Cannot read property file: $filename")
        null
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation(project(":ble-servo"))
}