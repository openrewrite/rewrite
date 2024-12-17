/*
 * Copyright 2021 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.table.ImageSourceFiles;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Reference;

import java.nio.file.Path;

public class FindImage extends Recipe {
    transient ImageSourceFiles results = new ImageSourceFiles(this);

    @Override
    public String getDisplayName() {
        return "Find files with images";
    }

    @Override
    public String getDescription() {
        return "Find files with container images like `image: eclipse-temurin:17-jdk-jammy`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFileWithReferences) {
                    SourceFileWithReferences sourceFile = (SourceFileWithReferences) tree;
                    Path sourcePath = sourceFile.getSourcePath();
                    for (Reference ref : sourceFile.getReferences().findMatches(new ImageMatcher())) {
                        results.insertRow(ctx, new ImageSourceFiles.Row(sourcePath.toString(), tree.getClass().getSimpleName(), ref.getValue()));
                        return SearchResult.found(tree, ref.getValue());
                    }
                }
                return tree;
            }
        };
    }
}
