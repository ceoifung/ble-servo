pluginManagement {
    repositories {
        google()
//        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
//        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "BleDemo"
include(":app")
include(":ble-servo")
