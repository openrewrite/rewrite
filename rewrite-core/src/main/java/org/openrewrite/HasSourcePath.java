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
package org.openrewrite;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public class HasSourcePath<P> extends TreeVisitor<Tree, P> {
    private final String syntax;
    private final String filePattern;

    public HasSourcePath(@Nullable String filePattern) {
        this("glob", filePattern);
    }

    /**
     * @param syntax      one of "glob" or "regex".
     * @param filePattern the file pattern.
     */
    public HasSourcePath(String syntax, @Nullable String filePattern) {
        this.syntax = syntax;
        this.filePattern = filePattern;
    }

    @Nullable
    @Override
    public Tree preVisit(Tree tree, P p) {
        stopAfterPreVisit();
        if (StringUtils.isBlank(filePattern)) {
            return SearchResult.found(tree, "has file");
        }

        if (tree instanceof SourceFile) {
            SourceFile sourceFile = (SourceFile) tree;
            Path sourcePath;
            if ("glob".equals(syntax) && filePattern.startsWith("**")) {
                sourcePath = Paths.get(".").resolve(sourceFile.getSourcePath().normalize());
            } else {
                sourcePath = sourceFile.getSourcePath().normalize();
            }

            PathMatcher pathMatcher = sourcePath.getFileSystem().getPathMatcher(syntax + ":" + filePattern);
            if (pathMatcher.matches(sourcePath)) {
                return SearchResult.found(sourceFile,"has file");
            }
        }

        return tree;
    }
}
