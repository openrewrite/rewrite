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
package org.openrewrite.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.UncaughtVisitorException;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicReference;

public class TreeVisitorAdapterInterceptorTest {
    @Test
    void interceptor() {
        //noinspection unchecked
        JavaVisitor<Integer> jv = TreeVisitorAdapterInterceptor.adapt(new Adaptable(), JavaVisitor.class);

        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}").get(0);
        jv.visit(cu, 0);
    }

    @Test
    void findUncaught() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}").get(0);

        AtomicReference<UncaughtVisitorException> e = new AtomicReference<>();
        new JavaVisitor<Integer>() {
            @Override
            public J visitIdentifier(J.Identifier ident, Integer p) {
                e.set(new UncaughtVisitorException(new IllegalStateException("boom"), getCursor()));
                return super.visitIdentifier(ident, p);
            }
        }.visit(cu, 0);

        //noinspection unchecked
        JavaVisitor<Integer> jv = TreeVisitorAdapterInterceptor.adapt(
                new FindUncaughtVisitorException(e.get()), JavaVisitor.class);

        cu = (J.CompilationUnit) jv.visitNonNull(cu, 0);
        System.out.println(cu.printAll());
    }

    @Test
    void usesMethod() {
        G.CompilationUnit cu = GroovyParser.builder().build().parse("class Test {}").get(0);

        //noinspection unchecked
        GroovyVisitor<Integer> gv = TreeVisitorAdapterInterceptor.adapt(
                new UsesMethod<>("java.util.List add(..)"), GroovyVisitor.class);

        cu = (G.CompilationUnit) gv.visitNonNull(cu, 0);
        System.out.println(cu.printAll());
    }
}

class Adaptable extends TreeVisitor<Tree, Integer> {
    @Override
    public Tree preVisit(Tree tree, Integer p) {
        System.out.println("pre-visited " + tree);
        return super.preVisit(tree, p);
    }
}
