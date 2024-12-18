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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.table.ImageSourceFiles;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Reference;
import org.openrewrite.trait.SimpleTraitMatcher;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Remove this file, we will use the `FindDockerImageUses` in the rewrite-docker module
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
                Tree t = super.visit(tree, ctx);

                if (tree instanceof SourceFileWithReferences) {
                    SourceFileWithReferences sourceFile = (SourceFileWithReferences) tree;
                    Path sourcePath = sourceFile.getSourcePath();
                    Collection<Reference> references = sourceFile.getReferences().findMatches(new ImageMatcher());

                    /*new JavaIsoVisitor<ExecutionContext>() {
                      vsit
                    };

                    new SimpleTraitMatcher<Reference>() {

                        @Override
                        protected @Nullable Reference test(Cursor cursor) {
                            return null;
                        }
                    }.asVisitor().visit(sourceFile, 0);*/

                    // TODO improve: `if (sourceFile instanceof PlainText && references.size() > 1)` then all markers are set at beginning
                    String value = references.stream()
                            .map(Reference::getValue)
                            .peek(it -> results.insertRow(ctx, new ImageSourceFiles.Row(sourcePath.toString(), tree.getClass().getSimpleName(), it)))
                            .sorted()
                            .collect(Collectors.joining("|"));
                    /*System.out.println(tree);
                    System.out.println(value);*/
                    return SearchResult.found(tree, value);
                }
                return tree;
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ReferenceFindSearchResultVisitor extends TreeVisitor<Tree, ExecutionContext> {
        Map<Tree, Reference> matches;

        @Override
        public Tree postVisit(Tree tree, ExecutionContext ctx) {
            Reference reference = matches.get(tree);
            if (reference != null && getCursor().equals(reference.getCursor())) {
                return SearchResult.found(tree, reference.getValue());
            }
            return tree;
        }
    }
}
