import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.3.71"
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

val kotlinVersion by extra("1.3.71")
val kotlinCoroutinesVersion by extra("1.3.5")

val discord4jVersion by extra("3.0.13")
val influxdbVersion by extra("1.6.0")
val kotlinLoggingVersion by extra("1.7.9")
val logbackVersion by extra("1.2.3")
val spekVersion by extra("2.0.10")

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinCoroutinesVersion")

    implementation("com.discord4j:discord4j-core:$discord4jVersion")

    implementation("com.influxdb:influxdb-client-java:$influxdbVersion")
    implementation("com.influxdb:influxdb-client-kotlin:$influxdbVersion")
    implementation("com.influxdb:flux-dsl:$influxdbVersion")

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
        kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
    }
}
