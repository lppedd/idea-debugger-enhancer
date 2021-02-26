@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  java
  id("org.jetbrains.intellij") version "0.7.2"
  kotlin("jvm") version "1.4.30"
}

group = "com.github.lppedd"
version = "0.1"

repositories {
  mavenCentral()
}


dependencies {
  compileOnly(kotlin("stdlib"))
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

intellij {
  version = "IU-192.5728.98"
  downloadSources = true
  setPlugins("java")
}

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  test {
    useJUnitPlatform()
  }

  val kotlinSettings: KotlinCompile.() -> Unit = {
    kotlinOptions.apiVersion = "1.3"
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf(
      "-Xno-call-assertions",
      "-Xno-receiver-assertions",
      "-Xno-param-assertions",
      "-Xjvm-default=all"
    )
  }

  compileKotlin(kotlinSettings)
  compileTestKotlin(kotlinSettings)

  patchPluginXml {
    version(project.version)
    sinceBuild("192.5728")
    untilBuild(null)
    pluginDescription(File("plugin-description.html").readText(Charsets.UTF_8))
  }
}
