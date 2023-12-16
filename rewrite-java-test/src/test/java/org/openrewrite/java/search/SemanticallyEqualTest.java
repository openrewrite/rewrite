/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemanticallyEqualTest {

    private final JavaParser javaParser = JavaParser.fromJavaVersion().build();

    @Test
    void compareAbstractMethods() {
        assertEqualToSelf(
          """
            abstract class A {
                abstract void m();
            }
            """
        );
    }

    @Test
    void compareClassModifierLists() {
        assertEqual(
          """
            public abstract class A {
            }
            """,
          """
            abstract public class A {
            }
            """
        );
    }

    @Test
    void compareLiterals() {
        assertEqual(
          """
            class A {
                int n = 1;
            }
            """,
          """
            class A {
                int n = 1;
            }
            """
        );
    }

    @Test
    void compareFieldAccess() {
        assertExpressionsEqual(
          """
            class T {
                int n = 1;
                int a = T.this.n;
                int b = T.this.n;
            }
            """
        );
        assertExpressionsEqual(
          """
            class T {
                int n = 1;
                int a = T.this.n;
                int b = this.n;
            }
            """
        );
        assertExpressionsEqual(
          """
            class T {
                int n = 1;
                int a = T.this.n;
                int b = n;
            }
            """
        );
    }

    private void assertEqualToSelf(@Language("java") String a) {
        assertEqual(a, a);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void assertEqual(@Language("java") String a, @Language("java") String b) {
        J.CompilationUnit cua = (J.CompilationUnit) javaParser.parse(a).findFirst().get();
        javaParser.reset();
        J.CompilationUnit cub = (J.CompilationUnit) javaParser.parse(b).findFirst().get();
        assertEqual(cua, cub);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private void assertExpressionsEqual(@Language(value = "java") String source) {
        J.CompilationUnit cu = (J.CompilationUnit) javaParser.parse(source).findFirst().get();
        javaParser.reset();

        JavaIsoVisitor<Map<String, J.VariableDeclarations.NamedVariable>> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<String, J.VariableDeclarations.NamedVariable> map) {
                map.put(variable.getSimpleName(), variable);
                return super.visitVariable(variable, map);
            }
        };

        Map<String, J.VariableDeclarations.NamedVariable> result = visitor.reduce(cu, new HashMap<>());
        assertEqual(
          Objects.requireNonNull(result.get("a").getInitializer()),
          Objects.requireNonNull(result.get("b").getInitializer())
        );
    }

    private void assertEqual(J a, J b) {
        assertTrue(SemanticallyEqual.areEqual(a, b));
    }
}
