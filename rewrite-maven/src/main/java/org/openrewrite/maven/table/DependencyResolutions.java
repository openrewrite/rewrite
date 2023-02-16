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
package org.openrewrite.maven.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class DependencyResolutions extends DataTable<DependencyResolutions.Row> {

    public DependencyResolutions(Recipe recipe) {
        super(recipe, Row.class, DependencyResolutions.class.getName(),
                "Dependency resolutions",
                "Latencies of individual dependency resolution requests and their outcomes.");
    }

    @Value
    public static class Row {
        @Option(displayName = "Group",
                description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.")
        String groupId;

        @Column(displayName = "Artifact",
                description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.")
        String artifactId;

        @Column(displayName = "Version",
                description = "The resolved version.")
        String version;

        @Column(displayName = "Repository URI",
                description = "The artifact repository that this dependency attempted to resolve from.")
        String repositoryUri;

        @Column(displayName = "HTTP status",
                description = "The HTTP status code of the response.")
        Integer httpStatus;

        @Column(displayName = "Latency (ms)",
                description = "The time in milliseconds that it took to resolve this dependency.")
        Long latencyMillis;
    }
}
