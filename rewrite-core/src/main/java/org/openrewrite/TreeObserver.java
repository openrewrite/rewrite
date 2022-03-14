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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Incubating(since = "7.20.0")
public interface TreeObserver {
    void propertyChanged(String property, Tree oldTree, Tree newTree, Object oldValue, Object newValue);

    final class Registration {
        private final TreeObserver observer;
        private final List<Predicate<Tree>> predicates = new ArrayList<>();

        public Registration(TreeObserver observer) {
            this.observer = observer;
        }

        public Registration register(@Nullable Tree tree) {
            if (tree != null) {
                predicates.add(t -> t == tree);
            }
            return this;
        }

        public Registration registerType(Class<? extends Tree> treeType) {
            predicates.add(t -> treeType.isAssignableFrom(t.getClass()));
            return this;
        }

        public Registration register(Predicate<Tree> predicate) {
            predicates.add(predicate);
            return this;
        }

        public boolean isObserved(@Nullable Tree tree) {
            if (tree == null) {
                return false;
            }
            for (Predicate<Tree> predicate : predicates) {
                if (predicate.test(tree)) {
                    return true;
                }
            }
            return false;
        }

        public TreeObserver getObserver() {
            return observer;
        }
    }

    static TreeObserver.Registration observeAll(Tree tree, TreeObserver.Registration observer) {
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree visit(@Nullable Tree tree, Integer p) {
                if (tree != null) {
                    observer.register(tree);
                }
                return super.visit(tree, p);
            }
        }.visit(tree, 0);

        return observer;
    }
}
