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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markup;
import org.openrewrite.table.ParseFailures;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindParseFailures extends Recipe {

    @Option(displayName = "Max snippet length",
            description = "When the failure occurs on a granular tree element, its source code will be included " +
                          "as a column in the data table up to this maximum snippet length.",
            required = false)
    @Nullable
    Integer maxSnippetLength;

    @Option(displayName = "Parser type",
            description = "Only display failures from parsers with this fully qualified name.",
            required = false,
            example = "org.openrewrite.yaml.YamlParser")
    @Nullable
    String parserType;

    @Option(displayName = "Stack trace",
            description = "Only mark stack traces with a message containing this text.",
            required = false)
    @Nullable
    String stackTrace;

    transient ParseFailures failures = new ParseFailures(this);

    @Override
    public String getDisplayName() {
        return "Find source files with `ParseExceptionResult` markers";
    }

    @Override
    public String getDescription() {
        return "This recipe explores parse failures after an LST is produced for classifying the types of " +
               "failures that can occur and prioritizing fixes according to the most common problems.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public Tree postVisit(Tree tree, ExecutionContext ctx) {
                return tree.getMarkers().findFirst(ParseExceptionResult.class)
                        .map(exceptionResult -> {
                            if (parserType != null && !Objects.equals(exceptionResult.getParserType(), parserType)) {
                                return tree;
                            }

                            if (stackTrace != null && !exceptionResult.getMessage().contains(stackTrace)) {
                                return tree;
                            }

                            String snippet = tree instanceof SourceFile ? null : tree.printTrimmed(getCursor());
                            if (snippet != null && maxSnippetLength != null && snippet.length() > maxSnippetLength) {
                                snippet = snippet.substring(0, maxSnippetLength);
                            }

                            failures.insertRow(ctx, new ParseFailures.Row(
                                    exceptionResult.getParserType(),
                                    (tree instanceof SourceFile ? (SourceFile) tree : getCursor().firstEnclosingOrThrow(SourceFile.class))
                                            .getSourcePath().toString(),
                                    exceptionResult.getExceptionType(),
                                    exceptionResult.getTreeType(),
                                    snippet,
                                    exceptionResult.getMessage()
                            ));

                            return Markup.info(tree, exceptionResult.getMessage());
                        })
                        .orElse(tree);
            }
        };
    }
}
