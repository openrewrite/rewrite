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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.tree.JavaType.ShallowClass.build;

@SuppressWarnings("ConstantConditions")
class MethodMatcherTest implements RewriteTest {
    private Pattern typeRegex(String signature) {
        return new MethodMatcher(signature).getTargetTypePattern();
    }

    private Pattern nameRegex(String signature) {
        return new MethodMatcher(signature).getMethodNamePattern();
    }

    private Pattern argRegex(String signature) {
        return new MethodMatcher(signature).getArgumentPattern();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void matchesSuperclassTypeOfInterfaces() {
        rewriteRun(
          java(
            "class Test { java.util.List l; }",
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    JavaType.FullyQualified listType = multiVariable.getTypeAsFullyQualified();
                    assertTrue(new MethodMatcher("java.util.Collection size()", true).matchesTargetType(listType));
                    assertFalse(new MethodMatcher("java.util.Collection size()").matchesTargetType(listType));
                    // ensuring subtypes do not match parents, regardless of matchOverrides
                    assertFalse(new MethodMatcher("java.util.List size()", true).matchesTargetType(build("java.util.Collection")));
                    assertFalse(new MethodMatcher("java.util.List size()").matchesTargetType(build("java.util.Collection")));
                    return multiVariable;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void matchesSuperclassTypeOfClasses() {
        assertTrue(new MethodMatcher("Object equals(Object)", true).matchesTargetType(build("java.lang.String")));
        assertFalse(new MethodMatcher("Object equals(Object)").matchesTargetType(build("java.lang.String")));
        // ensuring subtypes do not match parents, regardless of matchOverrides
        assertFalse(new MethodMatcher("String equals(String)", true).matchesTargetType(build("java.lang.Object")));
        assertFalse(new MethodMatcher("String equals(String)").matchesTargetType(build("java.lang.Object")));
    }

    @Test
    void matchesMethodTargetType() {
        assertTrue(typeRegex("*..MyClass foo()").matcher("com.bar.MyClass").matches());
        assertTrue(typeRegex("MyClass foo()").matcher("MyClass").matches());
        assertTrue(typeRegex("com.bar.MyClass foo()").matcher("com.bar.MyClass").matches());
        assertTrue(typeRegex("com.*.MyClass foo()").matcher("com.bar.MyClass").matches());
    }

    @Test
    void matchesMethodNameWithDotSeparator() {
        assertTrue(nameRegex("A.foo()").matcher("foo").matches());
        assertTrue(nameRegex("A.*()").matcher("foo").matches());
        assertTrue(nameRegex("A.fo*()").matcher("foo").matches());
    }

    @Test
    void matchesMethodNameWithPoundSeparator() {
        assertTrue(nameRegex("A#foo()").matcher("foo").matches());
        assertTrue(nameRegex("A#*()").matcher("foo").matches());
        assertTrue(nameRegex("A#fo*()").matcher("foo").matches());
        assertTrue(nameRegex("A#*oo()").matcher("foo").matches());
    }

    @Test
    void matchesMethodName() {
        assertTrue(nameRegex("A foo()").matcher("foo").matches());
        assertTrue(nameRegex("A *()").matcher("foo").matches());
        assertTrue(nameRegex("A fo*()").matcher("foo").matches());
        assertTrue(nameRegex("A *oo()").matcher("foo").matches());
    }

    @Test
    void matchesArguments() {
        assertTrue(argRegex("A foo()").matcher("").matches());
        assertTrue(argRegex("A foo(int)").matcher("int").matches());
        assertTrue(argRegex("A foo(java.util.Map)").matcher("java.util.Map").matches());
    }

    @Test
    void matchesUnqualifiedJavaLangArguments() {
        assertTrue(argRegex("A foo(String)").matcher("java.lang.String").matches());
    }

    @Test
    void matchesArgumentsWithWildcards() {
        assertTrue(argRegex("A foo(java.util.*)").matcher("java.util.Map").matches());
        assertTrue(argRegex("A foo(java..*)").matcher("java.util.Map").matches());
    }

    @Test
    void matchesArgumentsWithDotDot() {
        assertTrue(argRegex("A foo(.., int)").matcher("int").matches());
        assertTrue(argRegex("A foo(.., int)").matcher("int,int").matches());

        assertTrue(argRegex("A foo(int, ..)").matcher("int").matches());
        assertTrue(argRegex("A foo(int, ..)").matcher("int,int").matches());

        assertTrue(argRegex("A foo(..)").matcher("").matches());
        assertTrue(argRegex("A foo(..)").matcher("int").matches());
        assertTrue(argRegex("A foo(..)").matcher("int,int").matches());
    }

    @Test
    void matchesMethodSymbolsWithVarargs() {
        assertTrue(argRegex("A foo(String, Object...)").matcher("java.lang.String,java.lang.Object[]").matches());
    }

    @Test
    void dotDotMatchesArrayArgs() {
        assertTrue(argRegex("A foo(..)").matcher("java.lang.String,java.lang.Object[]").matches());
    }

    @Test
    void matchesArrayArguments() {
        assertTrue(argRegex("A foo(String[])").matcher("java.lang.String[]").matches());
    }

    @Test
    void matchesPrimitiveArgument() {
        assertTrue(argRegex("A foo(int)").matcher("int").matches());
        assertTrue(argRegex("A foo(int[])").matcher("int[]").matches());
        assertFalse(argRegex("A foo(int[])").matcher("int").matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.A <constructor>()", "a.A *()"})
    void matchesConstructorUsage(String methodPattern) {
        rewriteRun(
          java(
            """
              package a;
              class A {
                  A a = new A();
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu, methodPattern)).isNotEmpty())
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.A <constructor>()", "a.A *()"})
    void matchesMemberReferenceAsExpressionUsage(String methodPattern) {
        rewriteRun(
          java(
            """
              package a;
              import java.util.function.Supplier;
                            
              class A {
                  Supplier<A> a = A::new;
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu, methodPattern)).isNotEmpty())
          )
        );
    }

    @Test
    void matchesMethod() {
        rewriteRun(
          java(
            """
              package a;

              class A {
                  void setInt(int value) {}
                  int getInt() {}
                  void setInteger(Integer value) {}
                  Integer getInteger(){}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMethods.findDeclaration(cu, "a.A setInt(int)")).isNotEmpty();
                assertThat(FindMethods.findDeclaration(cu, "a.A getInt()")).isNotEmpty();
                assertThat(FindMethods.findDeclaration(cu, "a.A setInteger(Integer)")).isNotEmpty();
                assertThat(FindMethods.findDeclaration(cu, "a.A getInteger()")).isNotEmpty();
            })
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1215")
    void strictMatchMethodOverride() {
        rewriteRun(
          java(
            """
              package com.abc;

              class Parent {
                  public void method(String s) {
                  }
              }
                          
              class Test extends Parent {
                  @Override
                  public void method(String s) {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMethods.findDeclaration(cu, "com.abc.Parent method(String)", false)).hasSize(1);
                assertThat(FindMethods.findDeclaration(cu, "com.abc.Parent method(String)", true)).hasSize(2);
                assertThat(FindMethods.findDeclaration(cu, "com.abc.Test method(String)", false)).hasSize(1);
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/383")
    @Test
    void matchesMethodWithWildcardForClassInPackage() {
        rewriteRun(
          java(
            """
              package a;

              class A {
                  void foo() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.findDeclaration(cu, "* foo(..)")).isNotEmpty())
          )
        );
    }

    @Test
    void matchesMethodWithWildcardForClassNotInPackage() {
        rewriteRun(
          java(
            """
              class A {
                  void foo() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.findDeclaration(cu, "* foo(..)")).isNotEmpty())
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/492")
    @Test
    @SuppressWarnings("SpellCheckingInspection")
    void matchesWildcardedMethodNameStartingWithJavaKeyword() {
        assertTrue(nameRegex("A assert*()").matcher("assertThat").matches());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/629")
    @Test
    void wildcardType() {
        assertTrue(new MethodMatcher("*..* build()").matchesTargetType(build("javax.ws.rs.core.Response")));
        assertTrue(new MethodMatcher("javax..* build()").matchesTargetType(build("javax.ws.rs.core.Response")));
    }

    @Test
    void siteExample() {
        rewriteRun(
          java(
            """
              package com.yourorg;

              class Foo {
                  void bar(int i, String s) {}
                  void other() {
                      bar(0, "");
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu, "com.yourorg.Foo bar(int, String)")).isNotEmpty())
          )
        );
    }

    @Test
    void matchUnknownTypesNoSelect() {
        var mi = asMethodInvocation("assertTrue(Foo.bar());");
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesQualifiedStaticMethod() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar());");
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesPackageQualifiedStaticMethod() {
        var mi = asMethodInvocation("org.junit.Assert.assertTrue(Foo.bar());");
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesWildcardReceiverType() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar());");
        assertTrue(new MethodMatcher("*..* assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesFullWildcardReceiverType() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar());");
        assertTrue(new MethodMatcher("* assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesExplicitPackageWildcardReceiverType() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar());");
        assertTrue(new MethodMatcher("org.junit.* assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesRejectsMismatchedMethodName() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar());");
        assertFalse(new MethodMatcher("org.junit.Assert assertFalse(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesRejectsStaticSelectMismatch() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar());");
        assertFalse(new MethodMatcher("org.junit.FooAssert assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesRejectsTooManyArguments() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar(), \"message\");");
        assertFalse(new MethodMatcher("org.junit.Assert assertTrue(boolean)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesRejectsTooFewArguments() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar());");
        assertFalse(new MethodMatcher("org.junit.Assert assertTrue(boolean, String)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesRejectsMismatchingKnownArgument() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar(), \"message\");");
        assertFalse(new MethodMatcher("org.junit.Assert assertTrue(boolean, int)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesWildcardArguments() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar(), \"message\");");
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(..)").matches(mi, true));
    }

    @Test
    void matchUnknownTypesSingleWildcardArgument() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar(), \"message\");");
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(*, String)").matches(mi, true));
    }

    static J.MethodInvocation asMethodInvocation(String code) {
        var cu = JavaParser.fromJavaVersion().build().parse(
          String.format("""
            class MyTest {
                void test() {
                    %s
                }
            }
            """, code)
        ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
        var classDecl = cu.getClasses().get(0);
        J.MethodDeclaration testMethod = (J.MethodDeclaration) classDecl.getBody().getStatements().get(0);
        return (J.MethodInvocation) testMethod.getBody().getStatements().get(0);
    }

    @Test
    void arrayExample() {
        rewriteRun(
          java(
            """
              package com.yourorg;

              class Foo {
                  void bar(String[] s) {}
                  void test() {
                    bar(new String[] {"a", "b"});
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu, "com.yourorg.Foo bar(String[])")).isNotEmpty())
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2261")
    @Test
    void matcherForUnknownType() {
        rewriteRun(
          java(
            """
              class Test {
                  void foo(Unknown u) {}
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    assertThat(new MethodMatcher(MethodMatcher.methodPattern(method)).matches(method.getMethodType())).isTrue();
                    return super.visitMethodDeclaration(method, o);
                }
            })
          )
        );
    }
}
