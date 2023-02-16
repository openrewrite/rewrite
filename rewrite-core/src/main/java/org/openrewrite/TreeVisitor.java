/*
 * Copyright 2020 the original author or authors.
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
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.TreeVisitorAdapter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.beans.Transient;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Abstract {@link TreeVisitor} for processing {@link Tree elements}
 * <p>
 * Always returns input type T
 * provides Parameterizable P input which is mutable allowing context to be shared
 * <p>
 * postProcessing via afterVisit for conditionally chaining other operations with the expectation is that after
 * TreeVisitors are invoked immediately after visiting SourceFile
 *
 * @param <T> The type of tree.
 * @param <P> An input object that is passed to every visit method.
 */
public abstract class TreeVisitor<T extends Tree, P> {
    private Cursor cursor = new Cursor(null, Cursor.ROOT_VALUE);

    public static <T extends Tree, P> TreeVisitor<T, P> noop() {
        return new TreeVisitor<T, P>() {
            @Override
            public @Nullable T visit(@Nullable Tree tree, P p) {
                //noinspection unchecked
                return (T) tree;
            }

            @Override
            public @Nullable T visit(@Nullable Tree tree, P p, Cursor parent) {
                //noinspection unchecked
                return (T) tree;
            }
        };
    }

    private List<TreeVisitor<T, P>> afterVisit;

    private int visitCount;
    private final DistributionSummary visitCountSummary = DistributionSummary.builder("rewrite.visitor.visit.method.count").description("Visit methods called per source file visited.").tag("visitor.class", getClass().getName()).register(Metrics.globalRegistry);

    private ObjectDiffer differ;

    private ObjectDiffer getObjectDiffer() {
        if (differ == null) {
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
        }
        return differ;
    }

    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return true;
    }

    public void setCursor(@Nullable Cursor cursor) {
        this.cursor = cursor;
    }

    /**
     * @return Describes the language type that this visitor applies to, e.g. java, xml, properties.
     */
    @Nullable
    public String getLanguage() {
        return null;
    }

    /**
     * Execute the visitor once after the whole source file has been visited.
     * The visitor is executed against the whole source file. This operation only happens once
     * immediately after the containing visitor visits the whole source file. A subsequent {@link Recipe}
     * cycle will not run it.
     * <p>
     * This method is ideal for one-off operations like auto-formatting, adding/removing imports, etc.
     *
     * @param visitor The visitor to run.
     */
    protected void doAfterVisit(TreeVisitor<T, P> visitor) {
        afterVisit.add(visitor);
    }

    /**
     * Execute the recipe's main visitor once after the whole source file has been visited.
     * The visitor is executed against the whole source file. This operation only happens once
     * immediately after the containing visitor visits the whole source file. A subsequent {@link Recipe}
     * cycle will not run it.
     * <p>
     * This method is ideal for one-off operations like auto-formatting, adding/removing imports, etc.
     *
     * @param recipe The recipe whose visitor to run.
     */
    protected void doAfterVisit(Recipe recipe) {
        //noinspection unchecked
        afterVisit.add((TreeVisitor<T, P>) recipe.getVisitor());
    }

    protected List<TreeVisitor<T, P>> getAfterVisit() {
        return afterVisit;
    }

    public final Cursor getCursor() {
        return cursor;
    }

    @Nullable
    public T preVisit(T tree, P p) {
        return defaultValue(tree, p);
    }

    @Nullable
    public T postVisit(T tree, P p) {
        return defaultValue(tree, p);
    }

    @Nullable
    public T visit(@Nullable Tree tree, P p, Cursor parent) {
        this.cursor = parent;
        return visit(tree, p);
    }

    @Nullable
    public T visitSourceFile(SourceFile sourceFile, P p) {
        //noinspection unchecked
        return (T) sourceFile;
    }

    /**
     * By calling this method, you are asserting that you know that the outcome will be non-null
     * when the compiler couldn't otherwise prove this to be the case. This method is a shortcut
     * for having to assert the non-nullability of the returned tree.
     *
     * @param tree A non-null tree.
     * @param p    A state object that passes through the visitor.
     * @return A non-null tree.
     */
    public T visitNonNull(Tree tree, P p) {
        T t = visit(tree, p);
        assert t != null;
        return t;
    }

    public T visitNonNull(Tree tree, P p, Cursor parent) {
        T t = visit(tree, p, parent);
        assert t != null;
        return t;
    }

    @Incubating(since = "7.31.0")
    public static <R extends Tree, C extends Collection<R>> C collect(TreeVisitor<?, ExecutionContext> visitor,
                                                                      Tree tree, C initial) {
        //noinspection unchecked
        return collect(visitor, tree, initial, Tree.class, t -> (R) t);
    }

    @Incubating(since = "7.31.0")
    public static <U extends Tree, R, C extends Collection<R>> C collect(TreeVisitor<?, ExecutionContext> visitor,
                                                                         Tree tree, C initial, Class<U> matchOn,
                                                                         Function<U, R> map) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.addObserver(new TreeObserver.Subscription(new TreeObserver() {
            @Override
            public Tree treeChanged(Cursor cursor, Tree newTree) {
                initial.add(map.apply(matchOn.cast(newTree)));
                return newTree;
            }
        }).subscribeToType(matchOn));

        visitor.visit(tree, ctx);
        return initial;
    }

    @Incubating(since = "7.31.0")
    public P reduce(Iterable<? extends Tree> trees, P p) {
        for (Tree tree : trees) {
            visit(tree, p);
        }
        return p;
    }

    @Incubating(since = "7.31.0")
    public P reduce(Tree tree, P p) {
        visit(tree, p);
        return p;
    }

    @Incubating(since = "7.31.0")
    public P reduce(Tree tree, P p, Cursor parent) {
        visit(tree, p, parent);
        return p;
    }

    @Nullable
    public T visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        Timer.Sample sample = null;
        boolean topLevel = false;
        if (afterVisit == null) {
            topLevel = true;
            visitCount = 0;
            sample = Timer.start();
            if (p instanceof ExecutionContext) {
                cursor.putMessage("org.openrewrite.ExecutionContext", p);
            }
            afterVisit = new CopyOnWriteArrayList<>();
        }

        visitCount++;
        setCursor(new Cursor(cursor, tree));

        T t = null;
        // Do you visitor take tree and do you tree take visitor?
        boolean isAcceptable = tree.isAcceptable(this, p) && (!(tree instanceof SourceFile) || isAcceptable((SourceFile) tree, p));

        try {
            if (isAcceptable) {
                //noinspection unchecked
                t = preVisit((T) tree, p);
                if (t != null) {
                    t = t.accept(this, p);
                }
                if (t != null) {
                    t = postVisit(t, p);
                }
                if (t != tree && t != null && p instanceof ExecutionContext) {
                    ExecutionContext ctx = (ExecutionContext) p;
                    for (TreeObserver.Subscription observer : ctx.getObservers()) {
                        if (observer.isSubscribed(tree)) {
                            observer.getObserver().treeChanged(getCursor(), t);
                            DiffNode diff = getObjectDiffer().compare(t, tree);
                            AtomicReference<T> t2 = new AtomicReference<>(t);
                            diff.visit((node, visit) -> {
                                if (!node.hasChildren() && node.getPropertyName() != null) {
                                    //noinspection unchecked
                                    t2.set((T) observer.getObserver().propertyChanged(node.getPropertyName(), getCursor(), t2.get(), node.canonicalGet(tree), node.canonicalGet(t2.get())));
                                }
                            });
                            t = t2.get();
                        }
                    }
                }
            }

            setCursor(cursor.getParent());

            if (topLevel) {
                sample.stop(Timer.builder("rewrite.visitor.visit").tag("visitor.class", getClass().getName()).register(Metrics.globalRegistry));
                visitCountSummary.record(visitCount);

                if (t != null && afterVisit != null) {
                    for (TreeVisitor<T, P> v : afterVisit) {
                        if (v != null) {
                            v.setCursor(getCursor());
                            t = v.visit(t, p);
                        }
                    }
                }

                sample.stop(Timer.builder("rewrite.visitor.visit.cumulative").tag("visitor.class", getClass().getName()).register(Metrics.globalRegistry));
                afterVisit = null;
            }
        } catch (Throwable e) {
            if (e instanceof RecipeRunException) {
                // bubbling up from lower in the tree
                throw e;
            }

            throw new RecipeRunException(e, getCursor(), describeLocation(getCursor()));
        }

        //noinspection unchecked
        return isAcceptable ? t : (T) tree;
    }

    public void visit(@Nullable List<? extends T> nodes, P p) {
        if (nodes != null) {
            for (T node : nodes) {
                visit(node, p);
            }
        }
    }

    @SuppressWarnings("unused")
    @Nullable
    public T defaultValue(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T) tree;
    }

    @Incubating(since = "7.0.0")
    protected final <T2 extends Tree> T2 visitAndCast(T2 t, P p, BiFunction<T2, P, Tree> callSuper) {
        //noinspection unchecked
        return (T2) callSuper.apply(t, p);
    }

    @Incubating(since = "7.0.0")
    @Nullable
    protected final <T2 extends T> T2 visitAndCast(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T2) visit(tree, p);
    }

    public Markers visitMarkers(Markers markers, P p) {
        return markers.withMarkers(ListUtils.map(markers.getMarkers(), marker -> this.visitMarker(marker, p)));
    }

    @Incubating(since = "7.2.0")
    public <M extends Marker> M visitMarker(Marker marker, P p) {
        //noinspection unchecked
        return (M) marker;
    }

    public boolean isAdaptableTo(@SuppressWarnings("rawtypes") Class<? extends TreeVisitor> adaptTo) {
        if (adaptTo.isAssignableFrom(getClass())) {
            return true;
        }
        Class<? extends Tree> mine = visitorTreeType(getClass());
        Class<? extends Tree> theirs = visitorTreeType(adaptTo);
        return mine.isAssignableFrom(theirs);
    }

    @SuppressWarnings("rawtypes")
    private Class<? extends Tree> visitorTreeType(Class<? extends TreeVisitor> v) {
        for (TypeVariable<? extends Class<? extends TreeVisitor>> tp : v.getTypeParameters()) {
            for (Type bound : tp.getBounds()) {
                if (bound instanceof Class && Tree.class.isAssignableFrom((Class<?>) bound)) {
                    //noinspection unchecked
                    return (Class<? extends Tree>) bound;
                }
            }
        }

        Type sup = v.getGenericSuperclass();
        for (int i = 0; i < 20; i++) {
            if (sup instanceof ParameterizedType) {
                for (Type bound : ((ParameterizedType) sup).getActualTypeArguments()) {
                    if (bound instanceof Class && Tree.class.isAssignableFrom((Class<?>) bound)) {
                        //noinspection unchecked
                        return (Class<? extends Tree>) bound;
                    }
                }
                sup = ((ParameterizedType) sup).getRawType();
            } else if (sup instanceof Class) {
                sup = ((Class<?>) sup).getGenericSuperclass();
            }
        }
        throw new IllegalArgumentException("Expected to find a tree type somewhere in the type parameters of the " +
                                           "type hierarhcy of visitor " + getClass().getName());
    }

    public <R extends Tree, V extends TreeVisitor<R, P>> V adapt(Class<? extends V> adaptTo) {
        if (adaptTo.isAssignableFrom(getClass())) {
            //noinspection unchecked
            return (V) this;
        } else if (!isAdaptableTo(adaptTo)) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " must be adaptable to " + adaptTo.getName() + ".");
        }
        return TreeVisitorAdapter.adapt(this, adaptTo);
    }

    @Nullable
    protected String describeLocation(Cursor cursor) {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return null;
        }
        return sourceFile.getSourcePath().toString();
    }
}
