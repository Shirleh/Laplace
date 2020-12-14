import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    kotlin("kapt") version "1.4.21"
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.google.cloud.tools.jib") version "2.7.0"
}

group = "org.github.shirleh"
version = "0.1"

application {
    mainClassName = "com.github.shirleh.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
}

val kotlinVersion by extra("1.4.21")
val kotlinCoroutinesVersion by extra("1.4.1")

val arrowVersion by extra("0.11.0")
val koinVersion by extra("2.1.6")
val configVersion by extra("1.4.1")

val discord4jVersion by extra("3.1.3")
val cliktVersion by extra("3.1.0")
val emojiJavaVersion by extra("5.1.1")

val influxDBVersion by extra("1.14.0")
val exposedVersion by extra("0.28.1")
val sqliteVersion by extra("3.34.0")

val kotlinLoggingVersion by extra("2.0.3")
val logbackVersion by extra("1.2.3")
val spekVersion by extra("2.0.15")
val mockkVersion by extra("1.10.3")

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
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:${sqliteVersion}")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xinline-classes")
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

jib {
    from {
        image = "openjdk:11-jre-slim"
    }
    to {
        image = "laplace"
        tags = setOf("latest")
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
