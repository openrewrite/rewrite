/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.trait;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.trait.MavenDependency;
import org.openrewrite.maven.trait.MavenTraitMatcher;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class GradleDependency implements Trait<J.MethodInvocation> {
    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("* dependencies(groovy.lang.Closure)");
    private static final MethodMatcher DEPENDENCY_CONFIGURATION_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");


    Cursor cursor;
    ResolvedGroupArtifactVersion resolvedDependency;

    public static class Matcher extends SimpleTraitMatcher<GradleDependency> {
        @Nullable
        protected String groupId;

        @Nullable
        protected String artifactId;

        public Matcher groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Matcher artifactId(@Nullable String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        @Override
        protected @Nullable GradleDependency test(Cursor cursor) {
            if ((cursor.getValue() instanceof J.MethodInvocation)) {

                J.MethodInvocation methodInvocation = cursor.getValue();
                if (DEPENDENCY_CONFIGURATION_MATCHER.matches(methodInvocation)) {
                    // todo cant really use toString
                    String methodAsString = methodInvocation.toString();
                    if (methodAsString.contains("group") && methodAsString.contains("name")) {
                        Map<String, String> parts = new HashMap<>();
                        methodInvocation.getArguments()
                                .forEach(arg -> {
                                    G.MapEntry mapEntry = (G.MapEntry) arg;
                                    String key = mapEntry.getKey().toString();
                                    String value = mapEntry.getValue().toString();
                                    if (key.equals("group")) {
                                        parts.put("group", value);
                                    } else if (key.equals("name")) {
                                        parts.put("name", value);
                                    } else if (key.equals("version")) {
                                        parts.put("version", value);
                                    }
                                });

                        return new GradleDependency(
                                cursor,
                                new ResolvedGroupArtifactVersion(parts.get("group"), parts.get("name"), parts.get("version")));
                    } else {
                        // Format: implementation "commons-lang:commons-lang:2.6"
                        String gav = methodAsString.replaceAll("\"", "'");
                        int start = gav.indexOf("'") + 1;
                        int end = gav.lastIndexOf("'");
                        gav = gav.substring(start, end);
                        String[] parts = gav.split(":");
                        String group = parts[0];
                        String artifact = parts[1];
                        String version = parts[2];
                        return new GradleDependency(
                                cursor,
                                new ResolvedGroupArtifactVersion(group, artifact, version));
                    }
                }
                // If it's a configuration created by a plugin, we may not be able to type-attribute it
                // In the absence of type-attribution use its presence within a dependencies block to approximate
                // Todo - eventually try to look at gradle project and verify that the dep config is valid
                if (methodInvocation.getType() != null) {
                    return null;
                }

                while (cursor != null) {
                    if (cursor.getValue() instanceof J.MethodInvocation) {
                        J.MethodInvocation m = cursor.getValue();
                        String methodName = m.getSimpleName();
                        if ("constraints".equals(methodName) || "project".equals(methodName) || "modules".equals(methodName)
                                || "module".equals(methodName) || "file".equals(methodName) || "files".equals(methodName)) {
                            return null;
                        }
                        if (DEPENDENCY_DSL_MATCHER.matches(m)) {
                            // 1. String dsl - implementation "group:artifact:version"

                            // 2. Map dsl - implementation group: "group", name: "artifact", version: "version"

                            return new GradleDependency(cursor, new ResolvedGroupArtifactVersion("com.keap", "keap-api", "1.0.0"));
                        }
                    }
                    cursor = cursor.getParent();
                }
            }
            return null;
        }
    }
}
