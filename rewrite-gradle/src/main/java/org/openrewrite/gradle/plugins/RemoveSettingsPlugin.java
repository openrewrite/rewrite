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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsSettingsGradle;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveSettingsPlugin extends Recipe {
    @Option(displayName = "Plugin id",
            description = "The plugin id to remove.",
            example = "com.jfrog.bintray"
    )
    String pluginId;

    @Override
    public String getDisplayName() {
        return "Remove plugin from `settings.gradle(.kts)`";
    }

    @Override
    public String getDescription() {
        return "Remove plugin from `settings.gradle(.kts)`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsSettingsGradle<>();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemovePluginVisitor(pluginId);
    }
}
