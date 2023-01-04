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
package org.openrewrite.maven;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NormalizeMavenVariables extends Recipe {

    @Override
    public String getDisplayName() {
        return "Normalize Maven variables";
    }

    @Override
    public String getDescription() {
        return "Variables are all referenced by the prefix `project.`. You may also see references with `pom.` as the " +
               "prefix, or the prefix omitted entirely - these forms are now deprecated and should not be used.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            private final List<String> properties = Arrays.asList(
                    "basedir",
                    "groupId",
                    "artifactId",
                    "version",
                    "build.timestamp"
            );

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag t = super.visitTag(tag, executionContext);
                Optional<String> value = t.getValue();
                if (value.isPresent()) {
                    String newValue = value
                            .filter(v -> properties.stream().anyMatch(prop -> v.equals("${" + prop + "}")))
                            .map(v -> "${project." + v.substring(2))
                            .orElse(value.get());
                    return value.get().equals(newValue) ? t : t.withValue(newValue);
                }
                return t;
            }
        };
    }
}
