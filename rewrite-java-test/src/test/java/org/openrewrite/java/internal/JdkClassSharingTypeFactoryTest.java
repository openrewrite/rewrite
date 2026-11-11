/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class JdkClassSharingTypeFactoryTest {

    static {
        JdkClassSharingTypeFactory.useForParsers();
    }

    @Test
    void separatelyBuiltParsersShareTheClassesOfJavaPackages() {
        JavaType.Class first = fieldType(parse("class A { String s; }"));
        JavaType.Class second = fieldType(parse("class B { String s; }"));
        assertThat(second).isSameAs(first);
    }

    @Test
    void aCallHasTheMethodItsSharedClassDeclares() {
        parse("class C { int n(String s) { return s.length(); } }");
        JavaType.Method length = methodTypes(parse("class D { int n(String s) { return s.length(); } }")).get(0);
        assertThat(length.getDeclaringType().getMethods()).anyMatch(method -> method == length);
    }

    @Test
    void aSourceClassInAJavaPackageIsNotShared() {
        parse("package java.foo; public class Bar { }");
        J.CompilationUnit cu = parse("package java.foo; public class Bar { int x; }");
        assertThat(Objects.requireNonNull(cu.getClasses().get(0).getType()).getMembers())
          .extracting(JavaType.Variable::getName)
          .containsExactly("x");
    }

    private static J.CompilationUnit parse(String source) {
        return (J.CompilationUnit) JavaParser.fromJavaVersion().build().parse(source).findFirst().orElseThrow();
    }

    private static JavaType.Class fieldType(J.CompilationUnit cu) {
        J.VariableDeclarations field = (J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0);
        return (JavaType.Class) field.getType();
    }

    private static List<JavaType.Method> methodTypes(J.CompilationUnit cu) {
        return new JavaIsoVisitor<List<JavaType.Method>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<JavaType.Method> types) {
                types.add(Objects.requireNonNull(method.getMethodType()));
                return method;
            }
        }.reduce(cu, new ArrayList<>());
    }
}
