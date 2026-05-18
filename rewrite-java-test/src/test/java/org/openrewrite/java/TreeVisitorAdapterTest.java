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
import org.openrewrite.text.PlainTextVisitor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    /**
     * A single base visitor can have language-specific mixins registered from
     * multiple language modules (one extends KotlinIsoVisitor, another extends
     * GroovyVisitor, etc.). When adapting the base to {@code adaptTo} for one
     * language, registered mixins for other languages must be skipped — not
     * cause {@code adapt(...)} to throw — so dispatch for the current language
     * falls through to the base visitor.
     *
     * <p>Regression for: adapting {@code RemoveAnnotationVisitor} to
     * {@code GroovyVisitor} threw because {@code RemoveAnnotationKotlinMixin}
     * (registered for that base) extends {@code KotlinIsoVisitor}.
     */
    @Test
    void crossLanguageRegisteredMixinSkipped() {
        // META-INF/rewrite/mixins/org.openrewrite.java.TreeVisitorAdapterTest$RegistryAdaptable
        // registers JavaOnlyMixin (extends JavaIsoVisitor). Adapting to
        // PlainTextVisitor — which the Java mixin is not assignable to —
        // must silently skip the mixin rather than throw.
        assertThatCode(() ->
          //noinspection unchecked
          TreeVisitorAdapter.adapt(new RegistryAdaptable(), PlainTextVisitor.class)
        ).doesNotThrowAnyException();
    }

    public static class RegistryAdaptable extends TreeVisitor<Tree, Integer> {
    }

    public static class JavaOnlyMixin extends JavaIsoVisitor<Integer> {
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
