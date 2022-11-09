import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
//    application
    `java-library`
    `maven-publish`
}

group = "gov.cdc.dex"
version = "1.0.1-SNAPSHOT"

repositories {
    maven {
        url = uri("https://imagehub.cdc.gov/repository/maven-ede-group/")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.9.1")
    //Azure:
    implementation("com.azure:azure-messaging-eventhubs:5.14.0")
    implementation("redis.clients:jedis:4.3.1")

    testImplementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.18.0")

}

tasks.test {
    useJUnitPlatform()
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
            credentials(PasswordCredentials::class) //{
            //Add this to ~/.gradle/gradle.properties
//                username="$nexusUsername"
//                password="$nexusPassword"
//            }
        }


    }
}

