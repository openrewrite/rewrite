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

@Incubating(since = "7.25.0")
public class Applicability {

    public static <P> TreeVisitor<?, P> not(TreeVisitor<?, P> v) {
        return new TreeVisitor<Tree, P>() {
            @Override
            public Tree visit(@Nullable Tree tree, P p) {
                Tree t2 = v.visit(tree, p);
                return tree == t2 && tree != null ?
                        SearchResult.found(tree) :
                        tree;
            }
        };
    }

    @SafeVarargs
    public static <P> TreeVisitor<?, P> or(TreeVisitor<?, P>... vs) {
        return new TreeVisitor<Tree, P>() {
            @Override
            public Tree visit(@Nullable Tree tree, P p) {
                for (TreeVisitor<?, P> v : vs) {
                    Tree t2 = v.visit(tree, p);
                    if (tree != t2) {
                        return t2;
                    }
                }
                return tree;
            }
        };
    }

    @SafeVarargs
    public static <P> TreeVisitor<?, P> and(TreeVisitor<?, P>... vs) {
        return new TreeVisitor<Tree, P>() {
            @Override
            public Tree visit(@Nullable Tree tree, P p) {
                Tree t2 = tree;
                for (TreeVisitor<?, P> v : vs) {
                    t2 = v.visit(tree, p);
                    if (tree == t2) {
                        return tree;
                    }
                }
                return t2;
            }
        };
    }
}
