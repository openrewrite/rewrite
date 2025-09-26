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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.Validated;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.tree.JavaType.ShallowClass.build;

@SuppressWarnings("ConstantConditions")
class MethodMatcherTest implements RewriteTest {
    @SuppressWarnings("deprecation")
    private Pattern typeRegex(String signature) {
        return new MethodMatcher(signature).getTargetTypePattern();
    }

    @SuppressWarnings("deprecation")
    private Pattern nameRegex(String signature) {
        return new MethodMatcher(signature).getMethodNamePattern();
    }

    @SuppressWarnings("deprecation")
    private Pattern argRegex(String signature) {
        return new MethodMatcher(signature).getArgumentPattern();
    }

    @Test
    void invalidMethodMatcher() {
        Validated<String> validate = MethodMatcher.validate("com.google.common.collect.*");
        assertThat(validate.isValid()).isFalse();
        assertThat(validate.failures()).isNotEmpty();
        assertThat(validate.failures().getFirst().getMessage()).contains("mismatched input");
        assertThat(validate.failures().getFirst().getException()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void npmPackageNaming() {
        assertThat(MethodMatcher.validate("@types/lodash..* map(..)").isValid()).isTrue();
        assertTrue(typeRegex("@types/lodash..* map(..)").matcher("@types/lodash.LodashStatic").matches());
    }

    @Test
    void anyTypeMatchesNullTargetType() {
        assertTrue(new MethodMatcher("*..* equals(Object)", true).matchesTargetType(null));
        assertTrue(new MethodMatcher("*..* equals(Object)", true).matchesTargetType(JavaType.Unknown.getInstance()));
    }

    @SuppressWarnings("rawtypes")
    @Test
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
        assertTrue(nameRegex("*..*Service find*(..)").matcher("foo").matches());
        assertTrue(nameRegex("A.B.*()").matcher("foo").matches());
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
        assertTrue(argRegex("A foo(java..*)").matcher("java.util.Map").matches());
        assertTrue(argRegex("A foo(java.util.*)").matcher("java.util.Map").matches());
        assertTrue(argRegex("A foo(*.util.*)").matcher("java.util.Map").matches());
        assertTrue(argRegex("A foo(*..*)").matcher("java.util.Map").matches());
    }

    @Test
    void matchesExactlyOneWithWildcard() {
        assertTrue(argRegex("A foo(*)").matcher("int").matches());
        assertTrue(argRegex("A foo(*)").matcher("java.lang.String").matches());
        assertTrue(argRegex("A foo(*, int)").matcher("int,int").matches());
        assertTrue(argRegex("A foo(*,int)").matcher("int,int").matches());
        assertTrue(argRegex("A foo(*, int)").matcher("double,int").matches());
        assertTrue(argRegex("A foo(*, int)").matcher("java.lang.String,int").matches());
        assertTrue(argRegex("A foo(*, String)").matcher("java.lang.String,java.lang.String").matches());
        assertTrue(argRegex("A foo(int, *)").matcher("int,int").matches());
        assertTrue(argRegex("A foo(int, *)").matcher("int,double").matches());
        assertTrue(argRegex("A foo(int,*)").matcher("int,double").matches());
        assertTrue(argRegex("A foo(*, *)").matcher("int,int").matches());
        assertTrue(argRegex("A foo(*,*)").matcher("int,int").matches());
        assertTrue(argRegex("A foo(int, *, double)").matcher("int,int,double").matches());
        assertTrue(argRegex("A foo(int, *, double)").matcher("int,double,double").matches());
        assertTrue(argRegex("A foo(int,*,double)").matcher("int,double,double").matches());

        assertFalse(argRegex("A foo(*)").matcher("").matches());
        assertFalse(argRegex("A foo(*)").matcher("int,int").matches());
        assertFalse(argRegex("A foo(*, int)").matcher("int").matches());
        assertFalse(argRegex("A foo(*, int)").matcher("int,double").matches());
        assertFalse(argRegex("A foo(int, *)").matcher("int").matches());
        assertFalse(argRegex("A foo(int, *)").matcher("double,int").matches());
        assertFalse(argRegex("A foo(*, *)").matcher("").matches());
        assertFalse(argRegex("A foo(*, *)").matcher("int").matches());
        assertFalse(argRegex("A foo(int, *, double)").matcher("int,double").matches());
        assertFalse(argRegex("A foo(int, *, double)").matcher("double,int,double").matches());
    }

    @Test
    void matchesArgumentsWithDotDot() {
        assertTrue(argRegex("A foo(.., int)").matcher("int").matches());
        assertTrue(argRegex("A foo(.., int)").matcher("int,int").matches());
        assertTrue(argRegex("A foo(.., int)").matcher("double,int").matches());
        assertFalse(argRegex("A foo(.., int)").matcher("int,double").matches());

        assertTrue(argRegex("A foo(int, ..)").matcher("int").matches());
        assertTrue(argRegex("A foo(int, ..)").matcher("int,int").matches());
        assertTrue(argRegex("A foo(int, ..)").matcher("int,double").matches());
        assertFalse(argRegex("A foo(int, ..)").matcher("double,int").matches());

        assertTrue(argRegex("A foo(int, .., double)").matcher("int,double").matches());
        assertTrue(argRegex("A foo(int, .., double)").matcher("int,int,double").matches());
        assertTrue(argRegex("A foo(int, .., double)").matcher("int,double,double").matches());
        assertFalse(argRegex("A foo(int, .., double)").matcher("double,int,double").matches());

        assertTrue(argRegex("A foo(..)").matcher("").matches());
        assertTrue(argRegex("A foo(..)").matcher("int").matches());
        assertTrue(argRegex("A foo(..)").matcher("int,int").matches());

        assertTrue(argRegex("A foo(.., int)").matcher("double,double,int").matches());
        assertTrue(argRegex("A foo(int, .., double)").matcher("int,double,java.lang.String,int,double").matches());
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
    @ValueSource(strings = {"a.A <constructor>()", "a.A <init>()", "a.A *()"})
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1215")
    @SuppressWarnings("RedundantMethodOverride")
    @Test
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
    @SuppressWarnings("SpellCheckingInspection")
    @Test
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

    @Nested
    class UnknownTypes {

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
            assertTrue(new MethodMatcher("org.junit.Assert assertTrue(*, java.lang.String)").matches(mi, true));
            assertTrue(new MethodMatcher("org.junit.Assert assertTrue(String, *)").matches(mi, true));
            assertTrue(new MethodMatcher("org.junit.Assert assertTrue(double, *)").matches(mi, true));
            assertTrue(new MethodMatcher("org.junit.Assert assertTrue(java.lang.String, *)").matches(mi, true));
            assertTrue(new MethodMatcher("org.junit.Assert assertTrue(*, *)").matches(mi, true));
        }
    }


    @Issue("https://github.com/openrewrite/rewrite/pull/5833")
    @Test
    void matchKnownTypesSingleWildcardArgument() {
        var mi = asMethodInvocation("Assert.assertTrue(Foo.bar(), \"message\");", """
          class Foo {
              static String bar() {
                  return "bar";
              }
          }
          """);
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(*, String)").matches(mi, true));
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(*, java.lang.String)").matches(mi, true));
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(String, *)").matches(mi, true));
        assertFalse(new MethodMatcher("org.junit.Assert assertTrue(double, *)").matches(mi, true));
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(java.lang.String, *)").matches(mi, true));
        assertTrue(new MethodMatcher("org.junit.Assert assertTrue(*, *)").matches(mi, true));
    }

    static J.MethodInvocation asMethodInvocation(String code, @Language("java") String... dependsOn) {
        var cu = JavaParser.fromJavaVersion().dependsOn(dependsOn).build()
          .parse(
            """
              import org.junit.Assert;
              class MyTest {
                  void test() {
                      %s
                  }
              }
              """.formatted(code)
          )
          .findFirst()
          .map(J.CompilationUnit.class::cast)
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));
        var classDecl = cu.getClasses().getFirst();
        J.MethodDeclaration testMethod = (J.MethodDeclaration) classDecl.getBody().getStatements().getFirst();
        return (J.MethodInvocation) testMethod.getBody().getStatements().getFirst();
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
          spec -> spec.typeValidationOptions(TypeValidation.none()),
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

    @Test
    void failsToParse() {
        assertThatThrownBy(() -> new MethodMatcher("foo(|bar)"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void complexWildcardPatterns() {
        assertTrue(new MethodMatcher("*..*Service *find*(..)").matches(
          newMethodType("com.example.UserService", "findById")
        ));

        assertTrue(new MethodMatcher("*..*Controller *(.., javax.servlet.http.HttpServletResponse)").matches(
          newMethodType("com.example.web.UserController", "handleRequest",
            "java.lang.String",
            "javax.servlet.http.HttpServletResponse"
          )
        ));

        assertTrue(new MethodMatcher("org.springframework.web.bind.annotation.*Mapping *(..)").matches(
          newMethodType("org.springframework.web.bind.annotation.GetMapping", "value")
        ));

        assertTrue(new MethodMatcher("javax.servlet.http.HttpServlet do*(..)").matches(
          newMethodType("javax.servlet.http.HttpServlet", "doGet",
            "javax.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletResponse"
          ))
        );

        assertTrue(new MethodMatcher("java.util.concurrent.* submit(java.util.concurrent.Callable)").matches(
          newMethodType("java.util.concurrent.ExecutorService", "submit",
            "java.util.concurrent.Callable")
        ));
    }

    @Test
    void wildcardMethodNamePatterns() {
        assertTrue(new MethodMatcher("com.example.Service *find*(..)").matches(
          newMethodType("com.example.Service", "findUserById", "long")
        ));

        assertTrue(new MethodMatcher("com.example.Test assert*(..)").matches(
          newMethodType("com.example.Test", "assertEquals", "int", "int")
        ));

        assertTrue(new MethodMatcher("com.example.Foo *Bar(..)").matches(
          newMethodType("com.example.Foo", "fooBar")
        ));

        assertTrue(new MethodMatcher("com.example.Service *find*By*(..)").matches(
          newMethodType("com.example.Service", "findUserByEmail", "java.lang.String")
        ));
    }

    @Test
    void wildcardPackagePatterns() {
        assertTrue(new MethodMatcher("com.example..* *(..)").matches(
          newMethodType("com.example.my.deeply.nested.Service", "findUserById", "long")
        ));
    }

    @Test
    void wildcardMethodNamePatternsExactMatch() {
        // Test that print* matches both "print" exactly and methods starting with "print"
        assertTrue(new MethodMatcher("java.io.PrintStream print*(..)").matches(
          newMethodType("java.io.PrintStream", "print", "java.lang.String")
        ));
        assertTrue(new MethodMatcher("java.io.PrintStream print*(..)").matches(
          newMethodType("java.io.PrintStream", "println", "java.lang.String")
        ));
        assertTrue(new MethodMatcher("java.io.PrintStream print*(..)").matches(
          newMethodType("java.io.PrintStream", "printf", "java.lang.String", "java.lang.Object[]")
        ));

        // Test other exact match cases
        assertTrue(new MethodMatcher("com.example.Test assert*(..)").matches(
          newMethodType("com.example.Test", "assert")
        ));
        assertTrue(new MethodMatcher("com.example.Logger log*(..)").matches(
          newMethodType("com.example.Logger", "log", "java.lang.String")
        ));
        assertTrue(new MethodMatcher("com.example.Service find*(..)").matches(
          newMethodType("com.example.Service", "find")
        ));
    }

    @Test
    void multipleWildcardsInMethodName() {
        // Test the specific case from the request: get*Record* should match getRecords
        assertTrue(new MethodMatcher("org.springframework.kafka.test.utils.KafkaTestUtils get*Record*(.., long)").matches(
          newMethodType("org.springframework.kafka.test.utils.KafkaTestUtils", "getRecords",
            "org.apache.kafka.clients.consumer.Consumer", "long")
        ));

        // Test patterns with multiple wildcards where there's text between wildcards in both pattern and method name
        // Pattern: get*Records* should match getRecordsById (wildcards match "", "ById")
        assertTrue(new MethodMatcher("com.example.Test get*Records*(..)").matches(
          newMethodType("com.example.Test", "getRecordsById")
        ));

        // Pattern: find*By*Name should match findUserByName (wildcards match "User", "")
        assertTrue(new MethodMatcher("com.example.Service find*By*Name(..)").matches(
          newMethodType("com.example.Service", "findUserByName", "java.lang.String")
        ));

        // Pattern: get*And* should match getUserAndAccount (wildcards match "User", "Account")
        assertTrue(new MethodMatcher("com.example.Repository get*And*(..)").matches(
          newMethodType("com.example.Repository", "getUserAndAccount", "long", "long")
        ));

        // Test with wildcards at the start
        assertTrue(new MethodMatcher("com.example.Test *find*(..)").matches(
          newMethodType("com.example.Test", "prefixfindSuffix")
        ));
    }

    private static JavaType.Method newMethodType(String type, String method, String... parameterTypes) {
        List<JavaType> parameterTypeList = Stream.of(parameterTypes)
          .map(name -> {
              JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(name);
              return primitive != null ? primitive : JavaType.ShallowClass.build(name);
          })
          .map(JavaType.class::cast)
          .toList();

        return new JavaType.Method(
          null,
          1L,
          build(type),
          method,
          null,
          null,
          parameterTypeList,
          emptyList(),
          emptyList(),
          emptyList(),
          null
        );
    }

    @Nested
    class InnerClassMatching {

        @Test
        void matchesInnerClassWithDotPattern() {
            // Pattern with . should match types with $
            MethodMatcher matcher = new MethodMatcher("java.util.Map.Entry get*()");

            // Should match binary name with $
            assertTrue(matcher.matches(newMethodType("java.util.Map$Entry", "getKey")));
            assertTrue(matcher.matches(newMethodType("java.util.Map$Entry", "getValue")));

            // Should also match source name with .
            assertTrue(matcher.matches(newMethodType("java.util.Map.Entry", "getKey")));
        }

        @Test
        void matchesInnerClassWithDollarPattern() {
            // Pattern with $ should also work
            MethodMatcher matcher = new MethodMatcher("java.util.Map$Entry get*()");

            assertTrue(matcher.matches(newMethodType("java.util.Map$Entry", "getKey")));
            assertTrue(matcher.matches(newMethodType("java.util.Map.Entry", "getKey")));
        }

        @Test
        void matchesInnerClassWithWildcardPattern() {
            // Wildcard patterns should work with inner classes
            MethodMatcher matcher = new MethodMatcher("*..*Entry get*()");

            assertTrue(matcher.matches(newMethodType("java.util.Map$Entry", "getKey")));
            assertTrue(matcher.matches(newMethodType("java.util.Map.Entry", "getValue")));
            assertTrue(matcher.matches(newMethodType("com.example.Cache$Entry", "getKey")));
        }

        @Test
        void matchesNestedInnerClasses() {
            // Test deeply nested inner classes
            MethodMatcher matcher = new MethodMatcher("com.example.Outer.Middle.Inner method()");

            // Should match various representations
            assertTrue(matcher.matches(newMethodType("com.example.Outer$Middle$Inner", "method")));
            assertTrue(matcher.matches(newMethodType("com.example.Outer.Middle.Inner", "method")));

            // Mixed representation
            assertTrue(matcher.matches(newMethodType("com.example.Outer$Middle.Inner", "method")));
        }

        @Test
        void innerClassWildcardSuffix() {
            // Pattern like *Entry should only match simple class names without package separators
            MethodMatcher matcher = new MethodMatcher("*Entry get*()");

            // Should match simple class name
            assertTrue(matcher.matches(newMethodType("SomeEntry", "getValue")));
            assertTrue(matcher.matches(newMethodType("CacheEntry", "getKey")));

            // Should NOT match if there's a package separator (. or $)
            assertFalse(matcher.matches(newMethodType("java.util.Map$Entry", "getKey")));
            assertFalse(matcher.matches(newMethodType("com.example.Entry", "getKey")));

            // Pattern with .. should match inner classes
            MethodMatcher packageMatcher = new MethodMatcher("*..*Entry get*()");
            assertTrue(packageMatcher.matches(newMethodType("java.util.Map$Entry", "getKey")));
        }
    }

    @Nested
    class ToStringBehavior {
        @Test
        void preservesMethodNameWildcards() {
            // Test that toString() preserves method name patterns
            MethodMatcher matcher = new MethodMatcher("java.util.Map$Entry get*()");
            assertEquals("java.util.Map$Entry get*()", matcher.toString());

            // Test with various method name patterns
            assertEquals("java.io.PrintStream print*(..)",
                new MethodMatcher("java.io.PrintStream print*(..)").toString());

            assertEquals("*..*Service find*By*(..)",
                new MethodMatcher("*..*Service find*By*(..)").toString());

            assertEquals("com.example.* *get*()",
                new MethodMatcher("com.example.* *get*()").toString());
        }

        @Test
        void preservesExactMethodNames() {
            // Test exact method names are preserved
            assertEquals("java.lang.String substring(int)",
                new MethodMatcher("java.lang.String substring(int)").toString());

            assertEquals("java.util.List add(java.lang.Object)",
                new MethodMatcher("java.util.List add(java.lang.Object)").toString());
        }

        @Test
        void preservesConstructorPatterns() {
            // Test special method patterns
            assertEquals("com.example.Foo <constructor>()",
                new MethodMatcher("com.example.Foo <constructor>()").toString());

            assertEquals("com.example.Bar <constructor>(java.lang.String)",
                new MethodMatcher("com.example.Bar <init>(java.lang.String)").toString());
        }

        @Test
        void preservesWildcardArguments() {
            // Test wildcard arguments
            assertEquals("java.util.Map put(*, *)",
                new MethodMatcher("java.util.Map put(*, *)").toString());

            assertEquals("java.io.PrintStream println(..)",
                new MethodMatcher("java.io.PrintStream println(..)").toString());
        }
    }
}
