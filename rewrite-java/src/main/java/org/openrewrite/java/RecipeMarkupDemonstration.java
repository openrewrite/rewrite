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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markup;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RecipeMarkupDemonstration extends Recipe {

    @Option(displayName = "Level",
            description = "The `Markup#Level` to add.",
            valid = { "debug", "info", "warning", "error" })
    String level;

    @Override
    public String getDisplayName() {
        return "Demonstrate rendering of `Markup` markers.";
    }

    @Override
    public String getDescription() {
        return "Tooling may decide to elide or display differently markup of different levels.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return ListUtils.mapFirst(before, sourceFile -> {
            switch(level) {
                case "info":
                    return Markup.info(sourceFile, "This is an info message.");
                case "warning":
                    return Markup.warn(sourceFile, new IllegalStateException("This is a warning message."));
                case "error":
                    return Markup.error(sourceFile, new IllegalStateException("This is an error message."));
                case "debug":
                default:
                    return Markup.debug(sourceFile, "This is a debug message.");
            }
        });
    }
}
