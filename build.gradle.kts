plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("com.google.guava:guava:31.1-android")
            force("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}