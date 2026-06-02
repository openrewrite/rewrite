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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;

/**
 * Finds Gradle build files ending in `.gradle` and `.gradle.kts` files, but not `settings.gradle` or `settings.gradle.kts` files.
 * @param <P>
 */
public class IsBuildGradle<P> extends TreeVisitor<Tree, P> {

    /**
     * Only match `build.gradle` and `build.gradle.kts` files, not any `dependencies.gradle` or similar files.
     */
    boolean exactMatch;

    public IsBuildGradle() {
        this(false);
    }

    public IsBuildGradle(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    @Override
    public @Nullable Tree preVisit(@NonNull Tree tree, P p) {
        stopAfterPreVisit();
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile sourceFile = (JavaSourceFile) tree;
            Path sourcePath = sourceFile.getSourcePath();
            if (exactMatch) {
                return sourcePath.endsWith("build.gradle") || sourcePath.endsWith("build.gradle.kts") ? SearchResult.found(sourceFile) : tree;
            }
            if (matches(sourcePath)) {
                return SearchResult.found(sourceFile);
            }
        }
        return tree;
    }

    public static boolean matches(Path sourcePath) {
        return (sourcePath.toString().endsWith(".gradle") ||
                sourcePath.toString().endsWith(".gradle.kts")) &&
               !(sourcePath.toString().endsWith("settings.gradle") ||
                 sourcePath.toString().endsWith("settings.gradle.kts"));
    }
}
