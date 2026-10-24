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

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.plugin.publish)
  id("com.gradleup.shadow")
  id("atr-maven-bundle-java")
  id("atr-maven-bundle-publishing")
}

val readableName = "Buildish ATR Maven Bundle Plugin"

description = readableName

val repoUrl = "https://buildish.org/components/atr-maven-bundle"

gradlePlugin {
  website = repoUrl
  vcsUrl = repoUrl
  plugins {
    create("atrMavenBundle") {
      id = "org.buildish.atr-maven-bundle.gradle-plugin"
      displayName = readableName
      implementationClass = "org.buildish.atrmavenbundle.gradleplugin.AtrMavenBundlePlugin"
      description = project.description
      tags.addAll("publishing", "maven", "atr", "apache")
    }
  }
}

dependencies {}

tasks {
  shadowJar {
    exclude("META-INF/*.kotlin_module")
    enableAutoRelocation = true
    relocationPrefix = "org.buildish.atrmavenbundle.gradleplugin.shadow"
    manifest { attributes["Implementation-Version"] = project.version }
    archiveClassifier = ""
    outputs.cacheIf { false } // do not cache uber/shaded jars
  }

  jar.configure {
    dependsOn(shadowJar)
    archiveClassifier = "unshaded"
  }
}
