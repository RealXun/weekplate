plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.cleanroom.weekplate.backend.HouseholdServerKt")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
