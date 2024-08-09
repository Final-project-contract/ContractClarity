plugins {
    id("com.android.application") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}