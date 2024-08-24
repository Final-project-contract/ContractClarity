plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.server"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            kotlin.srcDirs("src/main/java")
        }
    }


}
configurations {
    create("serverRuntimeClasspath") {
        extendsFrom(configurations.getByName("implementation"))
        attributes {
            attribute(Attribute.of("org.gradle.usage", String::class.java), "java-runtime")
        }
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-gson:2.3.7")
    implementation("io.ktor:ktor-server-auth:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("io.ktor:ktor-server-cors:2.3.7")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.mindrot:jbcrypt:0.4")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-reload4j:1.7.36")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    // Explicitly add Guava
    implementation("com.google.guava:guava:31.1-android")

    implementation("org.jetbrains.exposed:exposed-jodatime:0.41.1")
    implementation("joda-time:joda-time:2.12.5")
    implementation("org.jetbrains.exposed:exposed-java-time:0.41.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
tasks.register<JavaExec>("runServer") {
    group = "Execution"
    description = "Run the Ktor server"
    classpath = files(android.bootClasspath) +
            files("${project.buildDir}/intermediates/javac/debug/classes") +
            files("${project.buildDir}/tmp/kotlin-classes/debug") +
            configurations.getByName("serverRuntimeClasspath")
    mainClass.set("com.example.server.ServerKt")
    workingDir = projectDir
    environment("PORT", "8080")
    environment("JWT_SECRET", "89VZJuRkKB0sglml")
    environment("DB_URL", "jdbc:postgresql://localhost:5432/contract_management")
    environment("DB_DRIVER", "org.postgresql.Driver")
    environment("DB_USER", "postgres")
    environment("DB_PASSWORD", "235689")
}

