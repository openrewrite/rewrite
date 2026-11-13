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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.TreeVisitorAdapter;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.scheduling.SourceFileDeadline;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

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
public abstract class TreeVisitor<T extends @Nullable Tree, P> implements Heavyweight {
    private static final String STOP_AFTER_PRE_VISIT = "__org.openrewrite.stopVisitor__";
    private static final int DEADLINE_CHECK_MASK = 255;

    // Built on first use, since a visitor started with a parent cursor never needs its own root. Private, so
    // that code in this package reading `cursor` (a Kotlin property) gets getCursor() and its pending levels.
    private @Nullable Cursor cursor;

    private static final ClassValue<Boolean> OVERRIDES_SET_CURSOR = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("setCursor", Cursor.class).getDeclaringClass() != TreeVisitor.class;
            } catch (NoSuchMethodException | LinkageError e) {
                // A signature names a class that is not on the classpath, so assume it does
                return true;
            }
        }
    };

    private final boolean overridesSetCursor = OVERRIDES_SET_CURSOR.get(getClass());

    /**
     * Values visited below {@link #cursor} that no one has asked a {@link Cursor} for yet, root-most first.
     * {@link #getCursor()} turns them into cursors, so the path a visitor observes is the same as if every
     * level had allocated one.
     */
    private Object @Nullable [] pending;

    private int pendingDepth;

    /**
     * Whether each level gets its cursor as it is entered. A class that overrides {@link #setCursor(Cursor)}
     * sees every move that way, and a visitor that asks for most of its cursors anyway switches to it.
     */
    private boolean eagerCursor = overridesSetCursor;

    private int materializedFrames;

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

    private @Nullable List<TreeVisitor<?, P>> afterVisit;

    private int visitCount;

    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return true;
    }

    public void setCursor(@Nullable Cursor cursor) {
        assert cursor != null;
        this.cursor = cursor;
        dropPending();
    }

    private void dropPending() {
        if (pendingDepth > 0) {
            //noinspection DataFlowIssue
            Arrays.fill(pending, 0, pendingDepth, null);
            pendingDepth = 0;
        }
    }

    /**
     * Enters {@code value} one level below the current position. Its cursor is built only if
     * {@link #getCursor()} is called while the level is entered.
     */
    protected final void pushCursor(Object value) {
        if (eagerCursor) {
            pushEager(value);
            return;
        }
        assert Cursor.isValidChild(pendingDepth > 0 ? pending[pendingDepth - 1] : getCursor().getValue(), value);
        Object[] p = pending;
        if (p == null) {
            p = pending = new Object[4];
        } else if (pendingDepth == p.length) {
            p = pending = Arrays.copyOf(p, p.length * 2);
        }
        p[pendingDepth++] = value;
    }

    private void pushEager(Object value) {
        if (overridesSetCursor) {
            setCursor(new Cursor(getCursor(), value));
        } else {
            cursor = new Cursor(getCursor(), value);
        }
    }

    /**
     * Leaves the level entered by the matching {@link #pushCursor(Object)}.
     */
    protected final void popCursor() {
        if (pendingDepth > 0) {
            //noinspection DataFlowIssue
            pending[--pendingDepth] = null;
        } else if (overridesSetCursor) {
            setCursor(cursor.getParent());
        } else {
            cursor = cursor.getParent();
            assert cursor != null : "popped past the root cursor";
        }
    }

    private void materialize() {
        materializedFrames += pendingDepth;
        // Building cursors on demand only pays when most of them are never asked for
        if (visitCount >= 64 && materializedFrames > visitCount / 2) {
            eagerCursor = true;
        }
        Object[] p = pending;
        Cursor c = cursor == null ? new Cursor(null, Cursor.ROOT_VALUE) : cursor;
        for (int i = 0; i < pendingDepth; i++) {
            //noinspection DataFlowIssue
            c = new Cursor(c, p[i]);
            p[i] = null;
        }
        cursor = c;
        pendingDepth = 0;
    }

    /**
     * @return Describes the language type that this visitor applies to, e.g. java, xml, properties.
     */
    public @Nullable String getLanguage() {
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
    protected void doAfterVisit(TreeVisitor<?, P> visitor) {
        if (afterVisit == null) {
            afterVisit = new ArrayList<>(2);
        }
        afterVisit.add(visitor);
    }

    protected List<TreeVisitor<?, P>> getAfterVisit() {
        return afterVisit == null ? emptyList() : afterVisit;
    }

    public final Cursor getCursor() {
        if (pendingDepth > 0) {
            materialize();
        } else if (cursor == null) {
            cursor = new Cursor(null, Cursor.ROOT_VALUE);
        }
        return cursor;
    }

    /**
     * Updates the cursor on this visitor and returns the updated cursor.
     *
     * @param currentValue The new (current) value of the cursor based on a mutation of what was formerly the
     *                     {@link Cursor#getValue()}.
     * @return The updated cursor.
     */
    public final Cursor updateCursor(T currentValue) {
        Cursor c = getCursor();
        Object old = c.getValue();
        if (!(old instanceof Tree)) {
            throw new IllegalArgumentException("To update the cursor, it must currently be positioned at a Tree instance");
        }
        if (!((Tree) old).getId().equals(requireNonNull(currentValue).getId())) {
            throw new IllegalArgumentException("Updating the cursor in place is only supported for mutations on a Tree instance " +
                                               "that maintain the same ID after the mutation.");
        }
        cursor = new Cursor(c.getParentOrThrow(), currentValue);
        return cursor;
    }

    public @Nullable T preVisit(@NonNull T tree, P p) {
        return defaultValue(tree, p);
    }

    public @Nullable T postVisit(@NonNull T tree, P p) {
        return defaultValue(tree, p);
    }

    public @Nullable T visit(@Nullable Tree tree, P p, Cursor parent) {
        this.cursor = parent;
        dropPending();
        return visit(tree, p);
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
    public @NonNull T visitNonNull(Tree tree, P p) {
        T t = visit(tree, p);
        assert t != null;
        return t;
    }

    public @NonNull T visitNonNull(Tree tree, P p, Cursor parent) {
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

    public @Nullable T visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        boolean topLevel = visitCount++ == 0;
        // A visit of a single node gets one cursor and no pending stack
        if (topLevel) {
            pushEager(tree);
        } else {
            pushCursor(tree);
        }

        T t = null;
        // Do you visitor take tree and do you tree take visitor?
        boolean isAcceptable = tree.isAcceptable(this, p) && (!(tree instanceof SourceFile) || isAcceptable((SourceFile) tree, p));

        try {
            // On every top-level visit, so short-lived visitors created in a loop still check, and every 256 visits
            if ((visitCount & DEADLINE_CHECK_MASK) == 1) {
                SourceFileDeadline.check();
            }
            if (isAcceptable) {
                //noinspection unchecked
                t = preVisit((T) tree, p);
                // A level that is still pending has no cursor, so no message on it either
                if (pendingDepth > 0 || !cursor.getMessage(STOP_AFTER_PRE_VISIT, false)) {
                    if (t != null) {
                        t = t.accept(this, p);
                    }
                    if (t != null) {
                        t = postVisit(t, p);
                    }
                }
                if (t != tree && t != null && p instanceof ExecutionContext) {
                    t = notifyObservers(tree, t, (ExecutionContext) p);
                }
            }

            popCursor();

            if (topLevel) {
                t = finishTopLevel(t, p);
            }
        } catch (Throwable e) {
            if (e instanceof RecipeRunException) {
                // bubbling up from lower in the tree
                throw e;
            }

            throw new RecipeRunException(e, getCursor());
        }

        //noinspection unchecked
        return isAcceptable ? t : (T) tree;
    }

    /**
     * Dispatches a changed tree to any subscribed {@link TreeObserver}s. Extracted from
     * {@link #visit(Tree, Object)} so that this rarely-taken path does not count against that
     * method's inlining budget.
     */
    private T notifyObservers(Tree tree, T t, ExecutionContext ctx) {
        for (TreeObserver.Subscription observer : ctx.getObservers()) {
            if (observer.isSubscribed(tree)) {
                t = observer.treeChanged(getCursor(), t, tree);
            }
        }
        return t;
    }

    /**
     * Runs the visitors registered via {@link #doAfterVisit(TreeVisitor)} and resets the per-visit state once
     * the top-level tree has been visited. It lives outside {@link #visit(Tree, Object)} to keep that method
     * within the JIT's inlining budget.
     */
    private @Nullable T finishTopLevel(@Nullable T t, P p) {
        if (t != null && afterVisit != null) {
            t = applyAfterVisitors(t, p);
        }
        afterVisit = null;
        visitCount = 0;
        materializedFrames = 0;
        eagerCursor = overridesSetCursor;
        return t;
    }

    // RewriteTest recognizes formatting done by an after-visitor by this method's name on the stack
    private @Nullable T applyAfterVisitors(@Nullable T t, P p) {
        for (TreeVisitor<?, P> v : afterVisit) {
            v.setCursor(getCursor());
            //noinspection unchecked
            t = (T) v.visit(t, p);
        }
        return t;
    }

    public void visit(@Nullable List<? extends T> nodes, P p) {
        if (nodes != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < nodes.size(); i++) {
                visit(nodes.get(i), p);
            }
        }
    }

    @SuppressWarnings("unused")
    public @Nullable T defaultValue(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T) tree;
    }

    @Incubating(since = "7.0.0")
    protected final <T2 extends Tree> T2 visitAndCast(T2 t, P p, BiFunction<T2, P, Tree> callSuper) {
        //noinspection unchecked
        return (T2) callSuper.apply(t, p);
    }

    @Incubating(since = "7.0.0")
    protected final <T2 extends T> @Nullable T2 visitAndCast(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T2) visit(tree, p);
    }

    public Markers visitMarkers(@Nullable Markers markers, P p) {
        if (markers == null || markers == Markers.EMPTY) {
            return Markers.EMPTY;
        } else if (markers.getMarkers().isEmpty()) {
            // avoid unnecessary method handle allocation
            return markers;
        }
        return markers.map(marker -> this.visitMarker(marker, p));
    }

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

    /**
     * Memoizes the (purely reflective) tree-type resolution per visitor class. Resolving the tree type
     * walks type variables and the generic superclass hierarchy, which is too expensive to repeat for
     * every node a non-adaptable visitor encounters. A {@link ClassValue} is used rather than a bounded
     * cache because the key space (visitor classes) is naturally bounded and tied to class lifecycle, so
     * it never needs eviction and does not pin classloaders.
     */
    @SuppressWarnings("rawtypes")
    private static final ClassValue<Class<? extends Tree>> TREE_TYPES = new ClassValue<Class<? extends Tree>>() {
        @Override
        protected Class<? extends Tree> computeValue(Class<?> v) {
            for (TypeVariable<?> tp : v.getTypeParameters()) {
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
                                               "type hierarchy of visitor " + v.getName());
        }
    };

    @SuppressWarnings("rawtypes")
    protected Class<? extends Tree> visitorTreeType(Class<? extends TreeVisitor> v) {
        return TREE_TYPES.get(v);
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

    /**
     * Can be used in {@link TreeVisitor#preVisit(Tree, Object)} to prevent a call
     * to {@link TreeVisitor#visit(Tree, Object)} and {@link TreeVisitor#postVisit(Tree, Object)}, therefore
     * stopping further navigation deeper down the tree.
     */
    @Incubating(since = "8.0.0")
    public void stopAfterPreVisit() {
        getCursor().putMessage(STOP_AFTER_PRE_VISIT, true);
    }
}
