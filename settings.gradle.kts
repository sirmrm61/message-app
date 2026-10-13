pluginManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "message-app"

include(":app")
include(":core")
include(":domain")
include(":data")
include(":classifier")
include(":feature-conversations")
include(":feature-chat")
include(":feature-promotions")
include(":feature-settings")
include(":tts")
