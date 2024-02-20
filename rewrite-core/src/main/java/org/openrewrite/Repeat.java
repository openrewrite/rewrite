/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.internal.lang.Nullable;

public class Repeat {

    /**
     * Returns a new visitor which runs the supplied visitor in a loop until no more changes are made, or a maximum
     * of 3 cycles is reached.
     * Convenient when a visitor is designed to recursively apply itself to a tree to achieve its desired result.
     * Stops early if the visitor ceases to make changes to the tree before the maximum number of cycles is reached.
     */
    public static TreeVisitor<?, ExecutionContext> repeatUntilStable(TreeVisitor<?, ExecutionContext> v) {
        return repeatUntilStable(v, 3);
    }

    /**
     * Returns a new visitor which runs the supplied visitor in a loop until no more changes are made, or the maximum
     * number of cycles is reached.
     * Convenient when a visitor is designed to recursively apply itself to a tree to achieve its desired result.
     * Stops early if the visitor ceases to make changes to the tree before the maximum number of cycles is reached.
     */
    public static TreeVisitor<?, ExecutionContext> repeatUntilStable(TreeVisitor<?, ExecutionContext> v, int maxCycles) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && !v.isAcceptable((SourceFile) tree, ctx)) {
                    return tree;
                }

                if (tree != null && !(tree instanceof SourceFile) && getCursor().isRoot()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Repeat visitor called on a non-source file tree without a cursor pointing to the root of the tree. " +
                                    "Passed tree type: `%s`. " +
                                    "This is likely a bug in the calling code. Use a `visit` method that accepts a cursor instead.",
                                    tree.getClass().getName()
                            ));
                }

                Tree previous = tree;
                Tree current = null;
                for (int i = 0; i < maxCycles; i++) {
                    current = v.visit(previous, ctx);
                    if (current == previous) {
                        break;
                    }
                    previous = current;
                }

                return current;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
                if (tree instanceof SourceFile && !v.isAcceptable((SourceFile) tree, ctx)) {
                    return tree;
                }

                Tree previous = tree;
                Tree current = null;
                for (int i = 0; i < maxCycles; i++) {
                    current = v.visit(previous, ctx, parent);
                    if (current == previous) {
                        break;
                    }
                    previous = current;
                }

                return current;
            }
        };
    }
}
