/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.InMemoryDiffEntry;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.ParseToPrintInequalities;
import org.openrewrite.tree.ParseError;

import java.util.Collections;

public class FindParseToPrintInequality extends Recipe {
    transient ParseToPrintInequalities inequalities = new ParseToPrintInequalities(this);

    @Override
    public String getDisplayName() {
        return "Find parse to print inequality";
    }

    @Override
    public String getDescription() {
        return "OpenRewrite `Parser` implementations should produce `SourceFile` objects whose `printAll()` " +
               "method should be byte-for-byte equivalent with the original source file. When this isn't true, " +
               "recipes can still run on the `SourceFile` and even produce diffs, but the diffs would fail to " +
               "apply as a patch to the original source file. Most `Parser` use `Parser#requirePrintEqualsInput` " +
               "to produce a `ParseError` when they fail to produce a `SourceFile` that is print idempotent.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof ParseError) {
                    ParseError parseError = (ParseError) tree;
                    if (parseError.getErroneous() != null) {
                        try (InMemoryDiffEntry diffEntry = new InMemoryDiffEntry(
                                parseError.getSourcePath(),
                                parseError.getSourcePath(),
                                null,
                                parseError.getText(),
                                parseError.getErroneous().printAll(),
                                Collections.emptySet()
                        )) {
                            inequalities.insertRow(ctx, new ParseToPrintInequalities.Row(
                                    parseError.getSourcePath().toString(),
                                    diffEntry.getDiff(false)
                            ));
                        }
                        return SearchResult.found(parseError);
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }
}
