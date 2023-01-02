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

import java.util.List;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindParseFailures extends Recipe {
    DataTable<ParseExceptionRow> failures = new DataTable<>("Parse failures", "A list of files that failed to parse.");

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
    public List<DataTable<?>> getDataTables() {
        return singletonList(failures);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visitSourceFile(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile.getMarkers().findFirst(ParseExceptionResult.class)
                        .<Tree>map(exceptionResult -> {
                            failures.insertRow(ctx, new ParseExceptionRow(
                                    sourceFile.getSourcePath().toString(),
                                    exceptionResult.getMessage()
                            ));
                            return Markup.info(sourceFile, exceptionResult.getMessage());
                        })
                        .orElse(sourceFile);
            }
        };
    }

    @Value
    public static class ParseExceptionRow {
        String sourcePath;
        String stackTrace;
    }
}
