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
import org.openrewrite.text.Find;
import org.openrewrite.text.PlainText;
import org.openrewrite.trait.Reference;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

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
                    Collection<Reference> references = sourceFile.getReferences().findMatches(new ImageMatcher());
                    Map<Tree, List<Reference>> matches = new HashMap<>();
                    for (Reference ref : references) {
                        results.insertRow(ctx, new ImageSourceFiles.Row(sourcePath.toString(), tree.getClass().getSimpleName(), ref.getValue()));
                        matches.computeIfAbsent(ref.getTree(), t -> new ArrayList<>()).add(ref);
                    }
                    return new ReferenceFindSearchResultVisitor(matches).visit(tree, ctx, getCursor());
                }
                return tree;
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ReferenceFindSearchResultVisitor extends TreeVisitor<Tree, ExecutionContext> {
        Map<Tree, List<Reference>> matches;

        @Override
        public Tree postVisit(Tree tree, ExecutionContext ctx) {
            List<Reference> references = matches.get(tree);
            if (references != null) {
                if (tree instanceof PlainText) {
                    String find = references.stream().map(Reference::getValue).sorted().collect(joining("|"));
                    return new Find(find, true, null, null, null, null, true)
                            .getVisitor()
                            .visitNonNull(tree, ctx);
                }
                return SearchResult.found(tree, references.get(0).getValue());
            }
            return tree;
        }
    }
}
