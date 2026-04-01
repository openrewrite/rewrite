/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddManagedPlugin extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Option(displayName = "Version",
            description = "A fixed version of the plugin to add.",
            example = "1.0.0",
            required = false)
    @Nullable
    String version;

    @Language("xml")
    @Option(displayName = "Configuration",
            description = "Optional plugin configuration provided as raw XML",
            example = "<configuration><foo>foo</foo></configuration>",
            required = false)
    @Nullable
    String configuration;

    @Language("xml")
    @Option(displayName = "Dependencies",
            description = "Optional plugin dependencies provided as raw XML.",
            example = "<dependencies><dependency><groupId>com.yourorg</groupId><artifactId>core-lib</artifactId><version>1.0.0</version></dependency></dependencies>",
            required = false)
    @Nullable
    String dependencies;

    @Language("xml")
    @Option(displayName = "Executions",
            description = "Optional executions provided as raw XML.",
            example = "<executions><execution><phase>generate-sources</phase><goals><goal>add-source</goal></goals></execution></executions>",
            required = false)
    @Nullable
    String executions;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                    "Multiple patterns may be specified, separated by a semicolon `;`. " +
                    "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " +
                    "When not set, all source files are searched. ",
            required = false,
            example = "**/*-parent/grpc-*/pom.xml")
    @Nullable
    String filePattern;

    String displayName = "Add Managed Maven plugin";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    String description = "Add the specified Maven plugin to the Plugin Managed of the pom.xml.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPluginVisitor(true, groupId, artifactId, version, configuration, dependencies, executions, filePattern);
    }
}
