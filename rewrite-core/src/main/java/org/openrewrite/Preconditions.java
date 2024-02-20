/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.marker.SearchResult;

public class Preconditions {

    public static TreeVisitor<?, ExecutionContext> check(Recipe check, TreeVisitor<?, ExecutionContext> v) {
        if (check instanceof ScanningRecipe) {
            throw new IllegalArgumentException("ScanningRecipe is not supported as a check");
        }
        return check(check.getVisitor(), v);
    }

    public static TreeVisitor<?, ExecutionContext> check(TreeVisitor<?, ExecutionContext> check, TreeVisitor<?, ExecutionContext> v) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return check.isAcceptable(sourceFile, ctx) && v.isAcceptable(sourceFile, ctx);
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                // if tree isn't an instanceof SourceFile, then a precondition visitor may
                // not be able to do its work because it may assume we are starting from the root level
                return !(tree instanceof SourceFile) || check.visit(tree, ctx) != tree ?
                        v.visit(tree, ctx) :
                        tree;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
                // if tree isn't an instanceof SourceFile, then a precondition visitor may
                // not be able to do its work because it may assume we are starting from the root level
                return !(tree instanceof SourceFile) || check.visit(tree, ctx, parent) != tree ?
                        v.visit(tree, ctx, parent) :
                        tree;
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> check(boolean check, TreeVisitor<?, ExecutionContext> v) {
        return check ? v : TreeVisitor.noop();
    }

    @Incubating(since = "8.0.0")
    @SafeVarargs
    public static TreeVisitor<?, ExecutionContext> firstAcceptable(TreeVisitor<?, ExecutionContext>... vs) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    for (TreeVisitor<?, ExecutionContext> v : vs) {
                        if (v.isAcceptable((SourceFile) tree, ctx)) {
                            return v.visit(tree, ctx);
                        }
                    }
                }
                return tree;
            }
        };
    }

    public static TreeVisitor<?, ExecutionContext> not(TreeVisitor<?, ExecutionContext> v) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = tree instanceof SourceFile ? (SourceFile) tree : null;
                // calling `isAcceptable()` in case `v` overrides `visit(Tree, P)`
                if (sourceFile != null && !v.isAcceptable(sourceFile, ctx)) {
                    return SearchResult.found(tree);
                }
                Tree t2 = v.visit(tree, ctx);
                return tree == t2 && tree != null ?
                        SearchResult.found(tree) :
                        tree;
            }
        };
    }

    @SafeVarargs
    public static TreeVisitor<?, ExecutionContext> or(TreeVisitor<?, ExecutionContext>... vs) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = tree instanceof SourceFile ? (SourceFile) tree : null;
                for (TreeVisitor<?, ExecutionContext> v : vs) {
                    // calling `isAcceptable()` in case `v` overrides `visit(Tree, P)`
                    if (sourceFile != null && !v.isAcceptable(sourceFile, ctx)) {
                        continue;
                    }
                    Tree t2 = v.visit(tree, ctx);
                    if (tree != t2) {
                        return t2;
                    }
                }
                return tree;
            }
        };
    }

    @SafeVarargs
    public static TreeVisitor<?, ExecutionContext> and(TreeVisitor<?, ExecutionContext>... vs) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = tree instanceof SourceFile ? (SourceFile) tree : null;
                Tree t2 = tree;
                for (TreeVisitor<?, ExecutionContext> v : vs) {
                    // calling `isAcceptable()` in case `v` overrides `visit(Tree, P)`
                    if (sourceFile != null && !v.isAcceptable(sourceFile, ctx)) {
                        continue;
                    }
                    t2 = v.visit(tree, ctx);
                    if (tree == t2) {
                        return tree;
                    }
                }
                return t2;
            }
        };
    }
}
