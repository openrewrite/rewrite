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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class JavaTreeShapeValidationServiceTest {

    static {
        JdkClassSharingTypeFactory.useForParsers();
    }

    private static final String SOURCE = "class A { org.jspecify.annotations.NullMarked m; b.B d; " +
                                         "Object o = java.util.Collections.emptyList(); }";

    @Test
    void untypedParserResolvesNoTypesWhileTheParserItCopiesStillDoes() {
        JavaParser.Builder<?, ?> typed = JavaParser.fromJavaVersion()
                .classpath("jspecify")
                .dependsOn("package b; public class B {}");
        J.ClassDeclaration untyped = parse(new JavaTreeShapeValidationService().untypedParser(typed));

        assertThat(TypeUtils.isOfClassType(fieldType(untyped, 0), "org.jspecify.annotations.NullMarked")).isFalse();
        assertThat(TypeUtils.isOfClassType(fieldType(untyped, 1), "b.B")).isFalse();
        assertThat(emptyList(untyped).getMethodType()).isNull();

        assertThat(TypeUtils.isOfClassType(fieldType(parse(typed), 0), "org.jspecify.annotations.NullMarked")).isTrue();
    }

    @Test
    void typedParserTakesTypesFromTheParserItCopiesButNotTheSourcesClassesAndGivesItNone() {
        JavaParser.Builder<?, ?> typed = JavaParser.fromJavaVersion();
        String source = "package a; class A { String s; void m() {} class B { void m() {} } }";
        J.CompilationUnit original = compilationUnit(typed, source);
        Parser.Builder reparser = new JavaTreeShapeValidationService().typedParser(typed, List.of(original));

        J.ClassDeclaration reparsed = parse(reparser, source.replace("m()", "renamed()"));
        assertThat(methods(reparsed)).containsExactly("renamed");
        assertThat(methods((J.ClassDeclaration) reparsed.getBody().getStatements().get(2))).containsExactly("renamed");
        assertThat(fieldType(reparsed, 0)).isSameAs(fieldType(original.getClasses().get(0), 0));

        assertThat(methods(parse(typed, source))).containsExactly("m");
    }

    @Test
    void typedParserGivesACallOfASharedClassTheMethodTheClassDeclares() {
        parse(JavaParser.fromJavaVersion(), "class P { java.util.List<String> l; }");
        JavaParser.Builder<?, ?> typed = JavaParser.fromJavaVersion();
        J.CompilationUnit original = compilationUnit(typed,
                "class A { boolean m(java.util.List<String> l) { return l.size() > 0; } }");
        Parser.Builder reparser = new JavaTreeShapeValidationService().typedParser(typed, List.of(original));

        J.ClassDeclaration reparsed = parse(reparser,
                "class A { boolean m(java.util.List<String> l) { return l.isEmpty(); } }");
        J.MethodDeclaration m = (J.MethodDeclaration) reparsed.getBody().getStatements().get(0);
        J.Return returned = (J.Return) requireNonNull(m.getBody()).getStatements().get(0);
        J.MethodInvocation call = (J.MethodInvocation) requireNonNull(returned.getExpression());
        JavaType.Method isEmpty = requireNonNull(call.getMethodType());
        assertThat(isEmpty.getDeclaringType().getMethods()).anyMatch(method -> method == isEmpty);
    }

    @Test
    void typedParserOfItsOwnTypesTakesNoneFromTheParserItCopies() {
        JavaParser.Builder<?, ?> typed = JavaParser.fromJavaVersion()
                .dependsOn("package b; public class B { public void m() {} }");
        parse(typed, "class A { void a(b.B b) { b.m(); } }");
        // a clone's cache reads through to that of the builder it copies, which holds B as it parsed it
        JavaParser.Builder<?, ?> next = typed.clone()
                .dependsOn("package b; public class B { public static void m() {} }");
        Parser.Builder reparser = new JavaTreeShapeValidationService().typedParser(next);

        J.ClassDeclaration reparsed = parse(reparser, "class A { void a() { b.B.m(); } }");
        J.MethodDeclaration a = (J.MethodDeclaration) reparsed.getBody().getStatements().get(0);
        J.MethodInvocation call = (J.MethodInvocation) requireNonNull(a.getBody()).getStatements().get(0);
        assertThat(requireNonNull(call.getMethodType()).hasFlags(Flag.Static)).isTrue();
    }

    @Test
    void comparesTheRestOfAMethodThatThrowsAnExceptionTheParserCannotResolve() {
        J.MethodDeclaration parsed = method("class A { int m() throws Missing { return 1; } }");
        J.MethodDeclaration recipes = parsed.withMethodType(requireNonNull(parsed.getMethodType())
                .withReturnType(JavaType.Primitive.Long));

        assertThat(new JavaTreeShapeValidationService().typeDifferences(recipes, parsed, true))
                .contains("the method long A.m() where the parser gives int A.m()");
    }

    @Test
    void writesAThrownExceptionTheParserCannotResolveAsUndefined() {
        J.MethodDeclaration parsed = method("class A { void m() throws Missing, java.io.IOException {} }");
        JavaType.Method methodType = requireNonNull(parsed.getMethodType());
        J.MethodDeclaration recipes = parsed.withMethodType(methodType.withThrownExceptions(
                List.of(methodType.getThrownExceptions().get(0), JavaType.ShallowClass.build("java.lang.Exception"))));

        assertThat(new JavaTreeShapeValidationService().typeDifferences(recipes, parsed, true)).containsExactly(
                "the method void A.m() throwing {undefined}, java.lang.Exception " +
                "where the parser gives void A.m() throwing {undefined}, java.io.IOException");
    }

    @Test
    void erasureLeavesOutTypeArgumentsFlagsAndThrownExceptions() {
        J.MethodDeclaration parsed = method("class A { java.util.List<String> m() throws java.io.IOException { return null; } }");
        JavaType.Method methodType = requireNonNull(parsed.getMethodType());
        JavaType.Parameterized listOfString = requireNonNull(TypeUtils.asParameterized(methodType.getReturnType()));
        J.MethodDeclaration recipes = parsed.withMethodType(methodType
                .withReturnType(listOfString.withTypeParameters(List.of(JavaType.ShallowClass.build("java.lang.Object"))))
                .withFlags(Set.of(Flag.Public, Flag.Static))
                .withThrownExceptions(List.of()));

        assertThat(new JavaTreeShapeValidationService().typeDifferences(recipes, parsed, true)).isNotEmpty();
        assertThat(new JavaTreeShapeValidationService().typeDifferences(recipes, parsed, false)).isEmpty();
    }

    @Test
    void erasureTakesANestedClassNamedWithADotAsExactDoes() {
        J.MethodDeclaration parsed = method("class A { Thread.State m() { return null; } }");
        J.MethodDeclaration recipes = parsed.withMethodType(requireNonNull(parsed.getMethodType())
                .withReturnType(JavaType.ShallowClass.build("java.lang.Thread.State")));

        assertThat(new JavaTreeShapeValidationService().typeDifferences(recipes, parsed, true)).isEmpty();
        assertThat(new JavaTreeShapeValidationService().typeDifferences(recipes, parsed, false)).isEmpty();
    }

    @Test
    void erasureComparesTheErasedClassATypeVariableAndAMultiCatch() {
        J.MethodDeclaration parsed = method("class A { java.util.List<String> m() { return null; } }");
        JavaType.Method methodType = requireNonNull(parsed.getMethodType());
        J.MethodDeclaration recipes = parsed.withMethodType(methodType.withReturnType(
                JavaType.ShallowClass.build("java.util.Collection")).withFlags(Set.of(Flag.Static)));

        assertThat(new JavaTreeShapeValidationService().typeDifferences(recipes, parsed, false))
                .contains("the method java.util.Collection A.m() where the parser gives java.util.List<java.lang.String> A.m()");

        J.MethodDeclaration generic = method("class A { <T> void m(T t) {} }");
        JavaType.Method genericType = requireNonNull(generic.getMethodType());
        J.MethodDeclaration takesObject = generic.withMethodType(genericType.withParameterTypes(
                List.of(JavaType.ShallowClass.build("java.lang.Object"))));
        assertThat(new JavaTreeShapeValidationService().typeDifferences(takesObject, generic, false))
                .anyMatch(difference -> difference.startsWith("the method "));

        J.MethodDeclaration takesU = generic.withMethodType(genericType.withParameterTypes(List.of(
                new JavaType.GenericTypeVariable(null, "U", JavaType.GenericTypeVariable.Variance.INVARIANT, null))));
        assertThat(new JavaTreeShapeValidationService().typeDifferences(takesU, generic, false))
                .anyMatch(difference -> difference.startsWith("the method "));

        J.MethodDeclaration bounded = method("class A { <T extends Number & Comparable<T>> void m(T t) {} }");
        JavaType.Method boundedType = requireNonNull(bounded.getMethodType());
        JavaType.GenericTypeVariable t = (JavaType.GenericTypeVariable) boundedType.getParameterTypes().get(0);
        J.MethodDeclaration boundedOtherwise = bounded.withMethodType(boundedType.withParameterTypes(List.of(
                t.withBounds(List.of(t.getBounds().get(0), JavaType.ShallowClass.build("java.io.Serializable"))))));
        assertThat(new JavaTreeShapeValidationService().typeDifferences(boundedOtherwise, bounded, false))
                .anyMatch(difference -> difference.startsWith("the method "));

        J.Try tryCatch = (J.Try) requireNonNull(method(
                "class A { void m() { try {} catch (IllegalStateException | IllegalArgumentException e) {} } }")
                .getBody()).getStatements().get(0);
        J.Identifier e = tryCatch.getCatches().get(0).getParameter().getTree().getVariables().get(0).getName();
        J.Identifier caught = e.withType(new JavaType.MultiCatch(List.of(
                JavaType.ShallowClass.build("java.lang.IllegalStateException"),
                JavaType.ShallowClass.build("java.lang.IllegalArgumentException"))));
        J.Identifier caughtElse = e.withType(new JavaType.MultiCatch(List.of(
                JavaType.ShallowClass.build("java.lang.RuntimeException"), JavaType.ShallowClass.build("java.lang.Error"))));
        assertThat(new JavaTreeShapeValidationService().typeDifferences(caughtElse, caught, false)).isNotEmpty();
        assertThat(new JavaTreeShapeValidationService().typeDifferences(caught.withType(new JavaType.MultiCatch(
                ((JavaType.MultiCatch) requireNonNull(caught.getType())).getThrowableTypes())), caught, false)).isEmpty();
    }

    private static J.MethodDeclaration method(String source) {
        return (J.MethodDeclaration) parse(JavaParser.fromJavaVersion(), source).getBody().getStatements().get(0);
    }

    private static J.ClassDeclaration parse(Parser.Builder parser) {
        return parse(parser, SOURCE);
    }

    private static J.ClassDeclaration parse(Parser.Builder parser, String source) {
        return compilationUnit(parser, source).getClasses().get(0);
    }

    private static J.CompilationUnit compilationUnit(Parser.Builder parser, String source) {
        return (J.CompilationUnit) parser.build()
                .parse(new InMemoryExecutionContext(), source)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private static List<String> methods(J.ClassDeclaration a) {
        return requireNonNull(a.getType()).getMethods().stream()
                .filter(m -> !m.isConstructor())
                .map(JavaType.Method::getName)
                .collect(toList());
    }

    private static @Nullable JavaType fieldType(J.ClassDeclaration a, int index) {
        return ((J.VariableDeclarations) a.getBody().getStatements().get(index)).getVariables().get(0).getType();
    }

    private static J.MethodInvocation emptyList(J.ClassDeclaration a) {
        J.VariableDeclarations o = (J.VariableDeclarations) a.getBody().getStatements().get(2);
        return (J.MethodInvocation) requireNonNull(o.getVariables().get(0).getInitializer());
    }
}
