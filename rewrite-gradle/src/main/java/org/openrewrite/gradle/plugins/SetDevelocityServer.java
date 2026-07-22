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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

@Value
@EqualsAndHashCode(callSuper = false)
public class SetDevelocityServer extends Recipe {

    String displayName = "Set the Develocity `server`";

    String description = "Sets the `server` in the `develocity` block of a Gradle settings file, adding it as the first " +
                         "statement when absent or updating it when it differs. Use this to point builds at a different " +
                         "Develocity server, for example when migrating between servers.";

    @Option(displayName = "Server",
            description = "The value to set for `server`.",
            example = "https://community.develocity.cloud/")
    String server;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Develocity.settingsBlockVisitor(block ->
                Develocity.setStringProperty(block, "server", server, null));
    }
}
