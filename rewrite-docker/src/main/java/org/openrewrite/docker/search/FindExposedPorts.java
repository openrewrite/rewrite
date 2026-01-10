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
package org.openrewrite.docker.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.table.DockerExposedPorts;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindExposedPorts extends Recipe {

    transient DockerExposedPorts exposedPorts = new DockerExposedPorts(this);

    @Option(displayName = "Port pattern",
            description = "A glob pattern to filter ports. For example, '80*' to find ports starting with 80. " +
                          "If not specified, all EXPOSE instructions are matched.",
            example = "80*",
            required = false)
    @Nullable
    String portPattern;

    @Override
    public String getDisplayName() {
        return "Find exposed ports";
    }

    @Override
    public String getDescription() {
        return "Find all EXPOSE instructions in Dockerfiles and report the exposed ports.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {

            @Override
            public Docker.Expose visitExpose(Docker.Expose expose, ExecutionContext ctx) {
                Docker.Expose e = super.visitExpose(expose, ctx);

                String sourceFile = getCursor().firstEnclosingOrThrow(Docker.File.class)
                        .getSourcePath().toString();

                // Get the stage name if available
                String stageName = null;
                Docker.Stage stage = getCursor().firstEnclosing(Docker.Stage.class);
                if (stage != null && stage.getFrom().getAs() != null) {
                    stageName = extractText(stage.getFrom().getAs().getName());
                }

                List<String> matchedPorts = new ArrayList<>();

                for (Docker.Argument portArg : e.getPorts()) {
                    String portSpec = extractText(portArg);
                    if (portSpec == null) {
                        continue; // Contains environment variables
                    }

                    // Parse port and protocol
                    String port;
                    String protocol = null;

                    int slashIndex = portSpec.indexOf('/');
                    if (slashIndex > 0) {
                        port = portSpec.substring(0, slashIndex);
                        protocol = portSpec.substring(slashIndex + 1).toLowerCase();
                    } else {
                        port = portSpec;
                    }

                    // Handle port ranges like 8000-8100
                    boolean isRange = port.contains("-");

                    // Check if port matches pattern
                    if (portPattern != null && !StringUtils.matchesGlob(portSpec, portPattern)) {
                        continue;
                    }

                    matchedPorts.add(portSpec);

                    exposedPorts.insertRow(ctx, new DockerExposedPorts.Row(
                            sourceFile,
                            stageName,
                            port,
                            protocol,
                            isRange
                    ));
                }

                if (!matchedPorts.isEmpty()) {
                    return SearchResult.found(e, String.join(", ", matchedPorts));
                }

                return e;
            }

            private @Nullable String extractText(Docker.@Nullable Argument arg) {
                if (arg == null) {
                    return null;
                }
                StringBuilder builder = new StringBuilder();
                for (Docker.ArgumentContent content : arg.getContents()) {
                    if (content instanceof Docker.PlainText) {
                        builder.append(((Docker.PlainText) content).getText());
                    } else if (content instanceof Docker.QuotedString) {
                        builder.append(((Docker.QuotedString) content).getValue());
                    } else if (content instanceof Docker.EnvironmentVariable) {
                        return null;
                    }
                }
                return builder.toString();
            }
        };
    }
}
