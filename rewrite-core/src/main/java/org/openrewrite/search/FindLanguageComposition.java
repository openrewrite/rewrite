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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.table.LanguageComposition;

import java.util.HashSet;
import java.util.Set;

public class FindLanguageComposition extends Recipe {
    transient LanguageComposition composition = new LanguageComposition(this);

    @Override
    public String getDisplayName() {
        return "Per-file language composition report";
    }

    @Override
    public String getDescription() {
        return "Produce a table of individual files, noting their language and size.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    Set<Integer> ids = new HashSet<>();
                    composition.insertRow(ctx, new LanguageComposition.Row(
                            sourceFile.getSourcePath().toString(),
                            sourceFile.getClass().getSimpleName(),
                            sourceFile.getWeight(id -> ids.add(System.identityHashCode(id))),
                            (int) sourceFile.printAll().chars().filter(c -> c == '\n').count()
                    ));
                }
                return tree;
            }
        };
    }
}
