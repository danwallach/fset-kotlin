import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
//    id("com.diffplug.spotless") version "5.12.4"
}

group = "edu.rice.fset"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("io.kotest:kotest-runner-junit5:4.5.0")
    testImplementation("io.kotest:kotest-assertions-core:4.5.0")
    testImplementation("io.kotest:kotest-property:4.5.0")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.5"
    apiVersion = "1.5"
}

//configure<com.diffplug.gradle.spotless.SpotlessExtension> {
//    kotlin {
//        // by default the target is every '.kt' and '.kts` file in the java sourcesets
//        ktlint()
//    }
//}