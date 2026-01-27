/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker.table;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class DockerExposedPorts extends DataTable<DockerExposedPorts.Row> {

    public DockerExposedPorts(Recipe recipe) {
        super(recipe,
                "Docker exposed ports",
                "Records all ports exposed in EXPOSE instructions in Dockerfiles.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The Dockerfile containing the EXPOSE instruction.")
        String sourceFile;

        @Column(displayName = "Stage name",
                description = "The build stage name if the EXPOSE is in a named stage.")
        @Nullable
        String stageName;

        @Column(displayName = "Port",
                description = "The port number or range (e.g., '80' or '8000-8100').")
        String port;

        @Column(displayName = "Protocol",
                description = "The protocol if specified (tcp or udp), null if not specified.")
        @Nullable
        String protocol;
    }
}
