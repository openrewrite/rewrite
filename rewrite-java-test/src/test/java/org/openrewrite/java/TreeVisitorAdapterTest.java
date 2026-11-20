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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.FindRecipeRunException;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.TreeVisitorAdapter;
import org.openrewrite.java.tree.J;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TreeVisitorAdapterTest {

    @Test
    void adapter() {
        AtomicInteger n = new AtomicInteger();
        //noinspection unchecked
        JavaVisitor<Integer> jv = TreeVisitorAdapter.adapt(new Adaptable(n), JavaVisitor.class);
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
        jv.visit(cu, 0);
        assertThat(n.get()).isEqualTo(4);
    }

    @Test
    void mixins() {
        AtomicInteger n = new AtomicInteger();
        CountingMixin mixin = new CountingMixin();
        mixin.n = n;
        //noinspection unchecked
        JavaVisitor<Integer> jv = TreeVisitorAdapter.adapt(
          new Adaptable(n),
          JavaVisitor.class,
          mixin
        );
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
        jv.visit(cu, 0);
        assertThat(n.get()).isEqualTo(
          /* Adaptable preVisit */ 4 +
            /* mixin preVisit */ 4 +
            /* mixin visitIdentifier */ 1);
    }

    /**
     * Mixins must be no-arg constructible so the Gizmo-generated proxy
     * (which extends the mixin class) can call {@code super()} at proxy
     * construction. {@code TreeVisitorAdapter} copies the user-provided
     * mixin instance's fields onto the proxy after instantiation, so
     * state set on the mixin (here, the shared counter) propagates.
     */
    public static class CountingMixin extends JavaIsoVisitor<Integer> {
        public AtomicInteger n;

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            n.incrementAndGet();
            return identifier;
        }

        @Override
        public J preVisit(J tree, Integer integer) {
            n.incrementAndGet();
            return tree;
        }
    }

    @Test
    void findUncaught() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

        AtomicReference<RecipeRunException> e = new AtomicReference<>();
        new JavaVisitor<Integer>() {
            @Override
            public J visitIdentifier(J.Identifier ident, Integer p) {
                e.set(new RecipeRunException(new IllegalStateException("boom"), getCursor()));
                return super.visitIdentifier(ident, p);
            }
        }.visit(cu, 0);

        //noinspection unchecked
        JavaVisitor<Integer> jv = TreeVisitorAdapter.adapt(
          new FindRecipeRunException(e.get()), JavaVisitor.class);

        jv.visitNonNull(cu, 0);
    }

    @Test
    void preVisitDeclaredOnSuperclassIsForwarded() {
        AtomicInteger n = new AtomicInteger();
        // preVisit is declared on the SUPERCLASS, not the leaf delegate class.
        //noinspection unchecked
        JavaVisitor<Integer> jv = TreeVisitorAdapter.adapt(new PreVisitSubclass(n), JavaVisitor.class);
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
        jv.visit(cu, 0);
        // Must fire per node exactly as if preVisit were declared on the leaf (see adapter()).
        assertThat(n.get()).isEqualTo(4);
    }

    @Test
    void visitMethodDeclaredOnIsoVisitorSuperclassIsForwarded() {
        AtomicInteger n = new AtomicInteger();
        // visitIdentifier is declared on a user superclass that extends JavaIsoVisitor; the adapter
        // must collect it (walking up to, but not into, JavaIsoVisitor).
        //noinspection unchecked
        JavaVisitor<Integer> jv = TreeVisitorAdapter.adapt(new IdentifierCountingSubclass(n), JavaVisitor.class);
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
        jv.visit(cu, 0);
        assertThat(n.get()).isEqualTo(1);
    }

    @Test
    void cachedAdapterStillCreatesAProxyPerDelegate() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        Adaptable firstDelegate = new Adaptable(first);
        //noinspection unchecked
        JavaVisitor<Integer> a1 = TreeVisitorAdapter.adapt(firstDelegate, JavaVisitor.class);
        //noinspection unchecked
        JavaVisitor<Integer> a2 = TreeVisitorAdapter.adapt(firstDelegate, JavaVisitor.class);
        //noinspection unchecked
        JavaVisitor<Integer> b = TreeVisitorAdapter.adapt(new Adaptable(second), JavaVisitor.class);

        assertThat(a1).isNotSameAs(a2).isNotSameAs(b);
        assertThat(a1.getClass()).isSameAs(a2.getClass()).isSameAs(b.getClass());

        a1.visit(cu, 0);
        b.visit(cu, 0);
        assertThat(first.get()).isEqualTo(4);
        assertThat(second.get()).isEqualTo(4);
    }

    @Test
    void adaptedVisitorSharesCursorWithDelegate() {
        Adaptable delegate = new Adaptable(new AtomicInteger());
        Cursor cursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), "parent");
        delegate.setCursor(cursor);

        //noinspection unchecked
        JavaVisitor<Integer> adapted = TreeVisitorAdapter.adapt(delegate, JavaVisitor.class);
        assertThat(adapted.getCursor()).isSameAs(cursor);

        Cursor moved = new Cursor(cursor, "child");
        adapted.setCursor(moved);
        assertThat(delegate.getCursor()).isSameAs(moved);
    }

    @Test
    void mixinStateIsCopiedPerAdaptation() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        CountingMixin firstMixin = new CountingMixin();
        firstMixin.n = first;
        CountingMixin secondMixin = new CountingMixin();
        secondMixin.n = second;

        //noinspection unchecked
        JavaVisitor<Integer> jv1 = TreeVisitorAdapter.adapt(new Adaptable(new AtomicInteger()), JavaVisitor.class, firstMixin);
        //noinspection unchecked
        JavaVisitor<Integer> jv2 = TreeVisitorAdapter.adapt(new Adaptable(new AtomicInteger()), JavaVisitor.class, secondMixin);
        jv1.visit(cu, 0);
        assertThat(first.get()).isEqualTo(/* mixin preVisit */ 4 + /* mixin visitIdentifier */ 1);
        assertThat(second.get()).isZero();

        jv2.visit(cu, 0);
        assertThat(second.get()).isEqualTo(5);
    }

    @Test
    void concurrentAdaptation() throws Exception {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> counts = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                counts.add(executor.submit(() -> {
                    AtomicInteger n = new AtomicInteger();
                    //noinspection unchecked
                    JavaVisitor<Integer> jv = TreeVisitorAdapter.adapt(new ConcurrentlyAdapted(n), JavaVisitor.class);
                    jv.visit(cu, 0);
                    return n.get();
                }));
            }
            for (Future<Integer> count : counts) {
                assertThat(count.get()).isEqualTo(4);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Recipe class loaders (e.g. the Moderne CLI's) give a recipe its own copy of rewrite-java, while the
     * visitor type a tree adapts to (e.g. {@code GroovyVisitor} from the LST's G.accept) comes from the parent.
     */
    @Test
    void adaptVisitorFromChildFirstClassLoader() throws Exception {
        URL rewriteJava = JavaIsoVisitor.class.getProtectionDomain().getCodeSource().getLocation();
        URL testClasses = TreeVisitorAdapterTest.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader recipeLoader = new ChildFirstClassLoader(new URL[]{rewriteJava, testClasses},
          TreeVisitorAdapterTest.class.getClassLoader())) {
            Class<?> childVisitor = recipeLoader.loadClass(ChildLoadedVisitor.class.getName());
            assertThat(childVisitor.getSuperclass()).isNotSameAs(JavaIsoVisitor.class);

            TreeVisitor<?, ?> delegate = (TreeVisitor<?, ?>) childVisitor.getDeclaredConstructor().newInstance();
            //noinspection unchecked
            JavaVisitor<Integer> adapted = TreeVisitorAdapter.adapt((TreeVisitor<J, ?>) delegate, JavaVisitor.class);
            J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}")
              .findFirst()
              .map(J.CompilationUnit.class::cast)
              .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
            assertThat(adapted.visit(cu, 0)).isSameAs(cu);
        }
    }

    public static class ChildLoadedVisitor extends JavaIsoVisitor<Integer> {
        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            return identifier;
        }
    }

    static class ChildFirstClassLoader extends URLClassLoader {
        ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(JavaIsoVisitor.class.getName()) && !name.startsWith(TreeVisitorAdapterTest.class.getName() + "$")) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        c = super.loadClass(name, resolve);
                    }
                }
                return c;
            }
        }
    }

    static class ConcurrentlyAdapted extends PreVisitBase {
        ConcurrentlyAdapted(AtomicInteger visitCount) {
            super(visitCount);
        }
    }

    static class IdentifierCountingBase extends JavaIsoVisitor<Integer> {
        final AtomicInteger n;

        IdentifierCountingBase(AtomicInteger n) {
            this.n = n;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            n.incrementAndGet();
            return identifier;
        }
    }

    static class IdentifierCountingSubclass extends IdentifierCountingBase {
        IdentifierCountingSubclass(AtomicInteger n) {
            super(n);
        }
    }
}

class PreVisitBase extends TreeVisitor<Tree, Integer> {
    final AtomicInteger visitCount;

    PreVisitBase(AtomicInteger visitCount) {
        this.visitCount = visitCount;
    }

    @Override
    public Tree preVisit(Tree tree, Integer p) {
        visitCount.incrementAndGet();
        return super.preVisit(tree, p);
    }
}

class PreVisitSubclass extends PreVisitBase {
    PreVisitSubclass(AtomicInteger visitCount) {
        super(visitCount);
    }
}

@Value
@EqualsAndHashCode(callSuper = false)
class Adaptable extends TreeVisitor<Tree, Integer> {
    AtomicInteger visitCount;

    @Override
    public Tree preVisit(Tree tree, Integer p) {
        visitCount.incrementAndGet();
        return super.preVisit(tree, p);
    }
}
