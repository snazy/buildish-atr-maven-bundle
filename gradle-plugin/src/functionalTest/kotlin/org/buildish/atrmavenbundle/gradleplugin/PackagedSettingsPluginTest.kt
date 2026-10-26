/*
 * Copyright 2026 The Buildish Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.buildish.atrmavenbundle.gradleplugin

import java.io.File
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PackagedSettingsPluginTest {
  @TempDir lateinit var projectDir: Path

  @Test
  fun typedSettingsDslLoadsThePublishedShadedPluginAndReusesConfigurationCache() {
    val repository = File(System.getProperty("plugin.repository"))
    val version = System.getProperty("plugin.version")
    val implementation = repository.resolve("org/buildish/atr-maven-bundle/gradle-plugin/$version")
    val jars = implementation.listFiles()!!.filter { it.extension == "jar" }
    val primaryJars = jars.filterNot {
      it.name.endsWith("-sources.jar") || it.name.endsWith("-javadoc.jar")
    }
    assertThat(primaryJars).hasSize(1)
    val jar = primaryJars.single()
    assertThat(jar.readBytes()).isEqualTo(File(System.getProperty("plugin.shaded.jar")).readBytes())
    assertThat(jars.map { it.name }).noneMatch {
      it.contains("unshaded") || it.contains("test-fixtures")
    }
    JarFile(jar).use { archive ->
      val pluginClass =
        archive.getJarEntry(
          "org/buildish/atrmavenbundle/gradleplugin/AtrMavenBundlePlugin.class",
        )
      val bytes = archive.getInputStream(pluginClass).use { it.readNBytes(8) }
      assertThat((bytes[6].toInt() and 255) * 256 + (bytes[7].toInt() and 255)).isEqualTo(61)
      assertThat(archive.entries().asSequence().map { it.name }.toList()).noneMatch {
        it.startsWith("kotlin/") || it.contains("/shadow/kotlin/")
      }
    }
    val metadata = implementation.listFiles()!!.single { it.extension == "module" }.readText()
    assertThat(metadata).contains("\"org.gradle.jvm.version\": 17")
    assertThat(metadata).doesNotContain("unshaded", "testFixtures", "kotlin-stdlib")
    val pom = implementation.listFiles()!!.single { it.extension == "pom" }.readText()
    assertThat(pom).doesNotContain("kotlin-stdlib")

    settings(
      """
      import org.buildish.atrmavenbundle.gradleplugin.AtrMavenBundleExtension

      plugins {
        id("org.buildish.atr-maven-bundle.gradle-plugin") version "$version"
      }
      rootProject.name = "packaged-consumer"
      include(":child")
      extensions.configure<AtrMavenBundleExtension>("atrMavenBundle") {
        check(requireSignatures.get())
        check(!signMissing.get())
        check(!baseName.isPresent)
        check(!bundleVersion.isPresent)
        excludeProject(":missing")
        excludePublication(":child", "experimental")
        requireSignatures.set(providers.provider { false })
        signMissing.set(true)
        baseName.set("release")
        bundleVersion.set("1.2.3")
        check(!requireSignatures.get() && signMissing.get())
        check(baseName.get() == "release" && bundleVersion.get() == "1.2.3")
      }
      println("Typed settings DSL configured")
      """
        .trimIndent(),
    )
    projectDir.resolve("child").createDirectories()
    projectDir.resolve("build.gradle.kts").writeText("")
    val first = runner().build()
    assertThat(first.output)
      .contains(
        "Typed settings DSL configured",
        "Configuration cache entry stored",
        "Isolated Projects",
      )
    val second = runner().build()
    assertThat(second.output).contains("Configuration cache entry reused")
  }

  @Test
  fun applyingTheEntryPointToAProjectFailsClearly() {
    settings("rootProject.name = \"unsupported-project-entry\"")
    projectDir
      .resolve("build.gradle.kts")
      .writeText(
        """
      plugins {
        id("org.buildish.atr-maven-bundle.gradle-plugin") version "${System.getProperty("plugin.version")}"
      }
      """
          .trimIndent(),
      )
    val failure = runner().buildAndFail()
    assertThat(failure.output).contains("org.buildish.atr-maven-bundle.gradle-plugin", "Settings")
  }

  private fun settings(body: String) {
    val repository = File(System.getProperty("plugin.repository")).toURI()
    projectDir
      .resolve("settings.gradle.kts")
      .writeText(
        """
      pluginManagement {
        repositories { maven { url = uri("$repository") } }
      }
      $body
      """
          .trimIndent(),
      )
  }

  private fun runner(): GradleRunner =
    GradleRunner.create()
      .withProjectDir(projectDir.toFile())
      .withGradleInstallation(File(System.getProperty("gradle.installation")))
      .withTestKitDir(File(System.getProperty("test.gradle.user.home"), "test-kit"))
      .withArguments(
        "help",
        "--stacktrace",
        "--configuration-cache",
        "--configuration-cache-problems=fail",
        "-Dorg.gradle.isolated-projects=true",
        "--gradle-user-home",
        System.getProperty("test.gradle.user.home"),
        "-Dorg.gradle.java.installations.auto-download=false",
      )
}
