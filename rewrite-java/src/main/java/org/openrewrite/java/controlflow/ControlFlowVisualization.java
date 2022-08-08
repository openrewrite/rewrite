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
package org.openrewrite.java.controlflow;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

@Value
@EqualsAndHashCode(callSuper = true)
public class ControlFlowVisualization extends Recipe {
    @Option(displayName = "Include Dotfile",
            description = "Also output with a Dotfile which can be then later visualized by Graphviz.",
            example = "true"
    )
    boolean includeDotfile;
    boolean darkMode;

    @Override
    public String getDisplayName() {
        return "Control Flow Visualization";
    }

    @Override
    public String getDescription() {
        return "Visualize the control flow of a Java program.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ControlFlowVisualizationVisitor<>(
                includeDotfile ? ControlFlowDotFileGenerator.create() : null,
                darkMode);
    }
}
