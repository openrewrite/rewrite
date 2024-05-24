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

import de.danielbechler.diff.ObjectDiffer;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.inclusion.Inclusion;
import de.danielbechler.diff.inclusion.InclusionResolver;
import de.danielbechler.diff.node.DiffNode;
import org.openrewrite.internal.lang.Nullable;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Incubating(since = "7.20.0")
public interface TreeObserver {
    default Tree treeChanged(Cursor cursor, Tree newTree) {
        return newTree;
    }

    default Tree propertyChanged(String property, Cursor cursor, Tree newTree, Object oldValue, Object newValue) {
        return newTree;
    }

    final class Subscription {
        private final TreeObserver observer;
        private final List<Predicate<Tree>> predicates = new ArrayList<>();

        @Nullable
        private final ObjectDiffer differ;

        public Subscription(TreeObserver observer) {
            this(observer, false);
        }

        public Subscription(TreeObserver observer, boolean diffChanges) {
            this.observer = observer;
            if (diffChanges) {
                differ = ObjectDifferBuilder.startBuilding()
                        .inclusion()
                        .resolveUsing(new InclusionResolver() {
                            @Override
                            public Inclusion getInclusion(DiffNode node) {
                                if (node.getPropertyAnnotation(Transient.class) != null) {
                                    return Inclusion.EXCLUDED;
                                }
                                return Inclusion.DEFAULT;
                            }

                            @Override
                            public boolean enablesStrictIncludeMode() {
                                return false;
                            }
                        })
                        .and()
                        .build();
            } else {
                differ = null;
            }
        }

        public Subscription subscribe(@Nullable Tree tree) {
            if (tree != null) {
                predicates.add(t -> t == tree);
            }
            return this;
        }

        public Subscription subscribeAll() {
            predicates.clear();
            predicates.add(t -> true);
            return this;
        }

        public Subscription subscribeAll(Tree tree) {
            new TreeVisitor<Tree, Integer>() {
                @Override
                public Tree visit(@Nullable Tree tree, Integer p) {
                    if (tree != null) {
                        subscribe(tree);
                    }
                    return super.visit(tree, p);
                }
            }.visit(tree, 0);

            return this;
        }

        public Subscription subscribeToType(Class<? extends Tree> treeType) {
            return subscribe(t -> treeType.isAssignableFrom(t.getClass()));
        }

        public Subscription subscribe(Predicate<Tree> predicate) {
            predicates.add(predicate);
            return this;
        }

        public boolean isSubscribed(@Nullable Tree tree) {
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

        public <T extends Tree> T treeChanged(Cursor cursor, T after, Tree before) {
            observer.treeChanged(cursor, after);
            if (differ != null) {
                return diff(after, before, cursor);
            }
            return after;
        }

        private <T extends Tree> T diff(T after, Tree before, Cursor cursor) {
            //noinspection DataFlowIssue
            DiffNode diff = differ.compare(after, before);
            AtomicReference<T> t2 = new AtomicReference<>(after);
            diff.visit((node, visit) -> {
                if (!node.hasChildren() && node.getPropertyName() != null) {
                    //noinspection unchecked
                    t2.set((T) observer.propertyChanged(node.getPropertyName(), cursor, t2.get(), node.canonicalGet(before), node.canonicalGet(t2.get())));
                }
            });
            return t2.get();
        }
    }
}
