/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class GradlePluginVersionResolutions extends DataTable<GradlePluginVersionResolutions.Row> {
    public GradlePluginVersionResolutions(Recipe recipe) {
        super(recipe, "Gradle plugin version resolutions",
                "Records the version resolution attempts made by UpgradePluginVersion, " +
                        "including which repositories were used and what version was resolved.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The source file where the plugin version was found.")
        String sourcePath;

        @Column(displayName = "Plugin id",
                description = "The plugin id that was being resolved.")
        String pluginId;

        @Column(displayName = "Current version",
                description = "The current version of the plugin before resolution.")
        String currentVersion;

        @Column(displayName = "Resolved version",
                description = "The version that was resolved, or empty if resolution failed.")
        String resolvedVersion;

        @Column(displayName = "Repositories",
                description = "The Maven repositories used for version resolution.")
        String repositories;

        @Column(displayName = "Message",
                description = "Additional diagnostic information, such as error messages.")
        String message;
    }
}
