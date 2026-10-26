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

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/** Settings entry point for configuring local Maven publication bundles. */
class AtrMavenBundlePlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    settings.extensions.create("atrMavenBundle", AtrMavenBundleExtension::class.java)
  }
}
