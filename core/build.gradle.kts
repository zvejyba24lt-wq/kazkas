plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

// Lists every text the app says, for the voice workflow (.github/workflows/garsai.yml).
tasks.register<JavaExec>("listAudio") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("lt.marcomultiservices.skaitaupats.core.ListAudioKt")
    args(rootProject.file("app/src/main/assets/kursas.json").absolutePath, rootProject.file("garsai/tekstai.txt").absolutePath)
}
