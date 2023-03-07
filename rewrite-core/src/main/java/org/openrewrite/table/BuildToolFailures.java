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
package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class BuildToolFailures extends DataTable<BuildToolFailures.Row> {
    public BuildToolFailures(Recipe recipe) {
        super(recipe,
                "Build tool failures",
                "Log output of failed build tool runs along with exit code and further diagnostics.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Type", description = "The type of build tool that failed.")
        String type;

        @Column(displayName = "Version", description = "The version of the build tool that failed, if available.")
        String version;

        @Column(displayName = "Command", description = "The command that was executed.")
        String command;

        @Column(displayName = "Exit code", description = "The exit code of the build tool run.")
        Integer exitCode;

        @Column(displayName = "Log output", description = "The log output of the build tool run.")
        String logOutput;
    }
}
