/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveBuildPlugin extends Recipe {
    @Option(displayName = "Plugin id",
            description = "The plugin id to remove.",
            example = "com.jfrog.bintray"
    )
    String pluginId;

    @Override
    public String getDisplayName() {
        return "Remove Gradle plugin";
    }

    @Override
    public String getDescription() {
        return "Remove plugin from Gradle `plugins` block by its id. Does not remove plugins from the `buildscript` block.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new RemovePluginVisitor(pluginId));
    }
}
