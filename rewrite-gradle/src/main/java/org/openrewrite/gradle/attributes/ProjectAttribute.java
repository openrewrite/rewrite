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
package org.openrewrite.gradle.attributes;

import lombok.Value;
import org.openrewrite.maven.attributes.Attribute;

/**
 * Marks a dependency as representing another Gradle project within the same multi-project build
 */
@Value
public class ProjectAttribute implements Attribute {
    /**
     * The path to the project within the gradle build.
     * Note that this is not a filesystem path. Delimiters are ":"
     * So the root project's path is ":"
     * A subproject may have a path like ":someDir:projectName"
     */
    String path;

    public static String key() {
        return "org.gradle.api.artifacts.ProjectDependency";
    }

    public static ProjectAttribute from(String path) {
        return new ProjectAttribute(path);
    }
}
