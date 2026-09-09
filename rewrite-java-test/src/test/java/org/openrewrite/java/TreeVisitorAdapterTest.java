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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.FindRecipeRunException;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.TreeVisitorAdapter;
import org.openrewrite.java.tree.J;

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
