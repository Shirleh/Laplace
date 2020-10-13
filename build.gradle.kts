import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
    kotlin("jvm") version "1.4.10"
    kotlin("kapt") version "1.4.10"
}

group = "org.github.shirleh"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "com.github.shirleh.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
}

val kotlinVersion by extra("1.4.10")
val kotlinCoroutinesVersion by extra("1.3.9")

val arrowVersion by extra("0.11.0")
val koinVersion by extra("2.1.6")
val configVersion by extra("1.4.0")

val discord4jVersion by extra("3.1.1")
val cliktVersion by extra("3.0.1")
val emojiJavaVersion by extra("5.1.1")
val influxDBVersion by extra("1.12.0")

val kotlinLoggingVersion by extra("2.0.3")
val logbackVersion by extra("1.2.3")
val spekVersion by extra("2.0.13")

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinCoroutinesVersion")

    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
    kapt("io.arrow-kt:arrow-meta:$arrowVersion")
    implementation("org.koin:koin-core:$koinVersion")
    implementation("com.typesafe:config:${configVersion}")

    implementation("com.discord4j:discord4j-core:$discord4jVersion")
    implementation("com.github.ajalt.clikt:clikt:${cliktVersion}")
    implementation("com.vdurmont:emoji-java:${emojiJavaVersion}")
    implementation("com.influxdb:influxdb-client-java:$influxDBVersion")
    implementation("com.influxdb:influxdb-client-kotlin:$influxDBVersion")
    implementation("com.influxdb:flux-dsl:$influxDBVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
    }
    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to application.mainClassName
                )
            )
        }
    }
}
