/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.marker.SearchResult;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Find JavaScript and TypeScript sources that are vendored, bundled or otherwise build output.
 * Intended as a negated precondition for recipes that should only change a project's own source.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class FindVendoredOrBundled extends Recipe {
    private static final List<String> PATH_PATTERNS = asList(
            "**/node_modules/**",
            "**/bower_components/**",
            "**/vendor/**",
            "**/dist/**",
            "**/*.min.*",
            "**/*.bundle.*"
    );

    String displayName = "Find vendored or bundled JavaScript";

    String description = "Find JavaScript and TypeScript sources that are vendored, bundled or build output, " +
                         "based on their path (`node_modules/`, `bower_components/`, `vendor/`, `dist/`, `*.min.*`, `*.bundle.*`) " +
                         "or a trailing `sourceMappingURL` comment.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof JS.CompilationUnit && isVendoredOrBundled((JS.CompilationUnit) tree)) {
                    return SearchResult.found(tree);
                }
                return tree;
            }
        };
    }

    private static boolean isVendoredOrBundled(JS.CompilationUnit cu) {
        for (String pattern : PATH_PATTERNS) {
            if (PathUtils.matchesGlob(cu.getSourcePath(), pattern)) {
                return true;
            }
        }
        List<Comment> comments = cu.getEof().getComments();
        if (!comments.isEmpty() && comments.get(comments.size() - 1) instanceof TextComment) {
            String text = ((TextComment) comments.get(comments.size() - 1)).getText().trim();
            return text.startsWith("# sourceMappingURL=") || text.startsWith("@ sourceMappingURL=");
        }
        return false;
    }
}
