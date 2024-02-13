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
package org.openrewrite.gradle;

import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class IsBuildGradle<P> extends TreeVisitor<Tree, P> {
    @Override
    public Tree visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            if (matches(cu.getSourcePath())) {
                return SearchResult.found(cu);
            }
        }
        return super.visit(tree, p);
    }

    public static boolean matches(Path sourcePath) {
        return (sourcePath.toString().endsWith(".gradle") ||
                sourcePath.toString().endsWith(".gradle.kts")) &&
               !(sourcePath.toString().endsWith("settings.gradle") ||
                 sourcePath.toString().endsWith("settings.gradle.kts"));
    }
}
