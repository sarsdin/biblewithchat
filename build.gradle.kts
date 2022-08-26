import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

group = "me.styne"
version = "1.0"

repositories {
    google()
    jcenter()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
//    implementation("io.ktor:ktor-server-netty:1.5.2")
//    implementation("io.ktor:ktor-html-builder:1.5.2")
//    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
      implementation("mysql:mysql-connector-java:8.0.30")

//    implementation ("androidx.core:core-ktx:1.8.0")
//    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
//    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10")
//    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    // Gson & 변환기 라이브러리
    implementation ("com.google.code.gson:gson:2.8.9")
//    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("com.biblewithchat.ChatServer")
}