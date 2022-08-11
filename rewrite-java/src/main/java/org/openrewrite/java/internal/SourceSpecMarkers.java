/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.internal;

import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.marker.JavaVersion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class SourceSpecMarkers {
    private static final Map<Integer, JavaVersion> javaVersions = new HashMap<>();
    private static final Map<String, JavaProject> javaProjects = new HashMap<>();
    private static final Map<String, JavaSourceSet> javaSourceSets = new HashMap<>();

    public static JavaVersion javaVersion(int version) {
        return javaVersions.computeIfAbsent(version, v ->
                new JavaVersion(Tree.randomId(), "openjdk", "adoptopenjdk",
                        Integer.toString(v), Integer.toString(v)));
    }

    public static JavaProject javaProject(String projectName) {
        return javaProjects.computeIfAbsent(projectName, name ->
                new JavaProject(Tree.randomId(), name, null));
    }

    public static JavaSourceSet javaSourceSet(String sourceSet) {
        return javaSourceSets.computeIfAbsent(sourceSet, name ->
                new JavaSourceSet(Tree.randomId(), name, Collections.emptyList()));
    }
}
