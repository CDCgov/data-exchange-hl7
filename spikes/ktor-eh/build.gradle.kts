val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.24"
    id("io.ktor.plugin") version "2.3.11"
}

group = "msc.edu"
version = "0.0.1"

application {
    mainClass.set("msc.edu.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
//    implementation("io.ktor:ktor-server-metrics-micrometer-jvm")
//    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")
    implementation("io.ktor:ktor-server-metrics-jvm")
//    implementation("io.github.flaxoos:ktor-server-kafka-jvm:1.+")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.insert-koin:koin-core:3.1.2")
    // Azure
    implementation("com.azure:azure-storage-blob:12.25.4")
    implementation("com.azure:azure-messaging-eventhubs:5.18.3")
    implementation("com.azure:azure-messaging-eventhubs-checkpointstore-blob:1.19.3")
    implementation("com.azure:azure-identity:1.8.0")
}
