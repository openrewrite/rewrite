/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc.internal;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;

/**
 * Used by RPC clients as edit and scan preconditions to efficiently skip
 * sending source files over the RPC connection that are not of interest to the recipe
 * through a simple type check.
 */
@RequiredArgsConstructor
public class FindTreesOfType extends TreeVisitor<Tree, ExecutionContext> {
    private final String type;
    private transient @Nullable Class<?> treeClass;

    @Override
    public Tree preVisit(@NonNull Tree tree, ExecutionContext ctx) {
        stopAfterPreVisit();
        return getType().isAssignableFrom(tree.getClass()) ?
                SearchResult.found(tree) : tree;
    }

    private Class<?> getType() {
        if (treeClass == null) {
            try {
                treeClass = Class.forName(type);
                if (!Tree.class.isAssignableFrom(treeClass)) {
                    throw new IllegalArgumentException(type + " is not a SourceFile type");
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return treeClass;
    }
}
