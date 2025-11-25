plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "com.pdf2md"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // CLI
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // PDF 처리
    implementation("org.apache.pdfbox:pdfbox:3.0.1")

    // 로깅
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // 테스트 - Kotest (Kotlin 네이티브)
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.pdf2md.MainKt")
}
