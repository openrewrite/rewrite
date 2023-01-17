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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.marker.Markup;
import org.openrewrite.table.ParseFailures;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindParseFailures extends Recipe {
    ParseFailures failures = new ParseFailures(this);

    @Override
    public String getDisplayName() {
        return "Find source files with `ParseExceptionResult` markers";
    }

    @Override
    public String getDescription() {
        return "This recipe explores parse failures after an AST is produced for classifying the types of " +
               "failures that can occur and prioritizing fixes according to the most common problems.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visitSourceFile(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile.getMarkers().findFirst(ParseExceptionResult.class)
                        .<Tree>map(exceptionResult -> {
                            failures.insertRow(ctx, new ParseFailures.Row(
                                    sourceFile.getSourcePath().toString(),
                                    exceptionResult.getMessage()
                            ));
                            return Markup.info(sourceFile, exceptionResult.getMessage());
                        })
                        .orElse(sourceFile);
            }
        };
    }
}
