import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
//    application
    `java-library`
    `maven-publish`
    jacoco
}

group = "gov.cdc.dex"
version = "1.0.19-SNAPSHOT"

repositories {
    maven {
        url = uri("https://imagehub.cdc.gov/repository/maven-ede-group/")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.10.1")
    //Azure:
    implementation("com.azure:azure-messaging-eventhubs:5.15.4")
    implementation("redis.clients:jedis:4.3.1")

    testImplementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.18.0")

}

tasks.test {
    useJUnitPlatform()
    //NOTE: ENVIRONMENT BLOCK MUST STAY IN THE SAME FORMAT AS BELOW - WILL BREAK CICD PIPELINE
    environment (mapOf("REDIS_CACHE_NAME" to "ocio-ede-dev-dex-cache.redis.cache.windows.net",
                       "REDIS_CACHE_KEY"  to findProperty("redisDevKey"),
                        "EVENT_HUB_CONNECT_STR" to findProperty("eventHubConnStr")
    ))

    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}


publishing {
    
    publications {
        create<MavenPublication>("myLibrary") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            val releasesRepoUrl  = "https://imagehub.cdc.gov/repository/maven-ede/"
            val snapshotsRepoUrl = "https://imagehub.cdc.gov/repository/maven-ede-snapshot/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            name = "nexus"
            credentials(PasswordCredentials::class){           
                username= System.getenv("IMAGEHUB_USERNAME")
                password= System.getenv("IMAGEHUB_PASSWORD")
           }
        }
    }
    
}

