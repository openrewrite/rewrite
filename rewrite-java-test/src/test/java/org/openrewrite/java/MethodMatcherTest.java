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
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.tree.JavaType.ShallowClass.build;

@SuppressWarnings("ConstantConditions")
class MethodMatcherTest implements RewriteTest {

    @Test
    void invalidMethodMatcher() {
        Validated<String> validate = MethodMatcher.validate("com.google.common.collect.*");
        assertThat(validate.isValid()).isFalse();
        assertThat(validate.failures()).isNotEmpty();
        assertThat(validate.failures().getFirst().getMessage()).contains("invalid method pattern");
        assertThat(validate.failures().getFirst().getException()).isInstanceOf(IllegalArgumentException.class);
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
        assertTrue(new MethodMatcher("A assert*()").matches(
          newMethodType("A", "assertThat")));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/629")
    @Test
    void wildcardType() {
        assertTrue(new MethodMatcher("*..* build()").matchesTargetType(build("javax.ws.rs.core.Response")));
        assertTrue(new MethodMatcher("javax..* build()").matchesTargetType(build("javax.ws.rs.core.Response")));
    }

    @Test
    void packagePrefixWithArrayDimensions() {
        JavaType arrayType = new JavaType.Array(null, build("javax.ws.rs.core.Response"), null);
        assertTrue(new MethodMatcher("javax.ws.rs..* process(javax.ws.rs..*[])").matches(
          new JavaType.Method(null, 1L, build("javax.ws.rs.core.ResponseBuilder"), "process",
            null, null, List.of(arrayType), emptyList(), emptyList(), emptyList(), null)));
    }

    @Test
    void packagePrefixWithVarargs() {
        JavaType arrayType = new JavaType.Array(null, build("javax.ws.rs.core.Response"), null);
        assertTrue(new MethodMatcher("javax.ws.rs..* process(javax.ws.rs..*...)").matches(
          new JavaType.Method(null, 1L, build("javax.ws.rs.core.ResponseBuilder"), "process",
            null, null, List.of(arrayType), emptyList(), emptyList(), emptyList(), null)));
    }

    @Test
    void varargsMatchesArrayType() {
        // Varargs parameters (String...) are represented as Array types (String[]) in JavaType
        JavaType.Array stringArray = new JavaType.Array(null, JavaType.Primitive.String, null);

        // Pattern with varargs should match Array type
        assertTrue(new MethodMatcher("com.example.Foo bar(String...)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(stringArray), emptyList(), emptyList(), emptyList(), null)));

        // Pattern with explicit array should also match
        assertTrue(new MethodMatcher("com.example.Foo bar(String[])").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(stringArray), emptyList(), emptyList(), emptyList(), null)));
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

        @Test
        void matchUnknownTypesWithWildcardInClassName() {
            var mi = asMethodInvocation("MyHelper.doSomething(true);");
            assertTrue(new MethodMatcher("com.foo.*Helper doSomething(boolean)").matches(mi, true));
            assertFalse(new MethodMatcher("com.foo.*Service doSomething(boolean)").matches(mi, true));
        }

        @Test
        void matchUnknownTypesWithPackageWildcardAndClassName() {
            var mi = asMethodInvocation("MyHelper.doSomething(true);");
            assertTrue(new MethodMatcher("com..MyHelper doSomething(boolean)").matches(mi, true));
            assertFalse(new MethodMatcher("com..OtherHelper doSomething(boolean)").matches(mi, true));
        }

        @Test
        void matchUnknownTypesWithClassNameWildcard() {
            var mi = asMethodInvocation("MyHelper.doSomething(true);");
            assertTrue(new MethodMatcher("com.foo.* doSomething(boolean)").matches(mi, true));
        }

        @Test
        void matchUnknownTypesExactClassNameMatch() {
            var mi = asMethodInvocation("MyHelper.doSomething(true);");
            assertTrue(new MethodMatcher("com.foo.MyHelper doSomething(boolean)").matches(mi, true));
            assertFalse(new MethodMatcher("com.foo.OtherHelper doSomething(boolean)").matches(mi, true));
        }

        @Test
        void matchUnknownTypesWithMultipleWildcardsInClassName() {
            var mi = asMethodInvocation("MyTestHelper.doSomething(true);");
            assertTrue(new MethodMatcher("com.foo.My*Helper doSomething(boolean)").matches(mi, true));
            assertFalse(new MethodMatcher("com.foo.Your*Helper doSomething(boolean)").matches(mi, true));
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
    void rejectsMultipleWildcardVarargs() {
        // Multiple .. wildcard varargs should be rejected
        assertThatThrownBy(() -> new MethodMatcher("com.example.Test foo(.., ..)"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("only one wildcard varargs (..) is allowed");

        // But mixing .. and ... should be allowed (different concepts)
        assertDoesNotThrow(() -> new MethodMatcher("com.example.Test foo(.., String...)"));
        assertDoesNotThrow(() -> new MethodMatcher("com.example.Test foo(int, .., String...)"));

        // Java varargs ... must be last, but can have multiple regular args or .. before it
        assertDoesNotThrow(() -> new MethodMatcher("com.example.Test foo(String, int, Object...)"));
    }

    @Test
    void allowsMixedWildcardAndVarargs() {
        // Test that patterns with both .. and ... are allowed (not rejected)
        // Note: The actual matching logic for mixed patterns is complex and would need more work
        assertDoesNotThrow(() -> {
            MethodMatcher matcher = new MethodMatcher("com.example.Test foo(.., String...)");
            // At minimum, the pattern should be parseable without throwing
            assertNotNull(matcher);
        });

        // Pattern with arguments before and after ..
        assertDoesNotThrow(() -> {
            MethodMatcher matcher = new MethodMatcher("com.example.Test foo(int, .., String...)");
            assertNotNull(matcher);
        });
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
        assertTrue(new MethodMatcher("com.example.Service *find*By*(..)").matches(
          newMethodType("com.example.Service", "refindByUser", "java.lang.String")
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

    @Test
    void mixedWildcardAndVarargsWithFindMethods() {
        // Test that patterns with both .. and ... work correctly with FindMethods
        rewriteRun(
          spec -> spec.recipe(new FindMethods("com.example.Logger log(.., Object...)", false)),
          java(
            """
              package com.example;
              
              class Logger {
                  // Method that takes any number of args followed by Object varargs
                  void log(String level, String message, Object... params) {
                      // logging implementation
                  }
              
                  void log(Object... params) {
                      // simpler logging
                  }
              
                  void log(int code, String level, String message, Object... params) {
                      // with error code
                  }
              }
              
              class Test {
                  void test() {
                      Logger logger = new Logger();
              
                      // Should match - level, message, then varargs
                      logger.log("INFO", "Hello", "world", 123);
              
                      // Should match - just varargs
                      logger.log("simple", "message");
              
                      // Should match - int, level, message, then varargs
                      logger.log(500, "ERROR", "Server error", new Exception());
              
                      // Should match - with no varargs params
                      logger.log("DEBUG", "No params");
                  }
              }
              """,
            """
              package com.example;
              
              class Logger {
                  // Method that takes any number of args followed by Object varargs
                  void log(String level, String message, Object... params) {
                      // logging implementation
                  }
              
                  void log(Object... params) {
                      // simpler logging
                  }
              
                  void log(int code, String level, String message, Object... params) {
                      // with error code
                  }
              }
              
              class Test {
                  void test() {
                      Logger logger = new Logger();
              
                      // Should match - level, message, then varargs
                      /*~~>*/logger.log("INFO", "Hello", "world", 123);
              
                      // Should match - just varargs
                      /*~~>*/logger.log("simple", "message");
              
                      // Should match - int, level, message, then varargs
                      /*~~>*/logger.log(500, "ERROR", "Server error", new Exception());
              
                      // Should match - with no varargs params
                      /*~~>*/logger.log("DEBUG", "No params");
                  }
              }
              """
          )
        );
    }

    @Test
    void varargsMethodInvocationWithMultipleArguments() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("com.example.Util format(String, Object...)", false)),
          java(
            """
              package com.example;
              
              class Util {
                  static String format(String template, Object... args) {
                      return String.format(template, args);
                  }
              }
              
              class Test {
                  void test() {
                      String result1 = Util.format("Hello %s %d", "world", 42);
                      String result2 = Util.format("No args");
                      String result3 = Util.format("Single: %s", "arg");
                  }
              }
              """,
            """
              package com.example;
              
              class Util {
                  static String format(String template, Object... args) {
                      return String.format(template, args);
                  }
              }
              
              class Test {
                  void test() {
                      String result1 = /*~~>*/Util.format("Hello %s %d", "world", 42);
                      String result2 = /*~~>*/Util.format("No args");
                      String result3 = /*~~>*/Util.format("Single: %s", "arg");
                  }
              }
              """
          )
        );
    }

    @Test
    void multiDimensionalArrayMatching() {
        // Test 1D array
        JavaType.Array stringArray = new JavaType.Array(null, JavaType.Primitive.String, null);
        assertTrue(new MethodMatcher("com.example.Foo bar(String[])").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(stringArray), emptyList(), emptyList(), emptyList(), null)));

        // Test 2D array
        JavaType.Array stringArray2D = new JavaType.Array(null, stringArray, null);
        assertTrue(new MethodMatcher("com.example.Foo bar(String[][])").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(stringArray2D), emptyList(), emptyList(), emptyList(), null)));

        // Test 3D array
        JavaType.Array stringArray3D = new JavaType.Array(null, stringArray2D, null);
        assertTrue(new MethodMatcher("com.example.Foo bar(String[][][])").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(stringArray3D), emptyList(), emptyList(), emptyList(), null)));

        // Test mismatch: pattern expects 1D but got 2D
        assertFalse(new MethodMatcher("com.example.Foo bar(String[])").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(stringArray2D), emptyList(), emptyList(), emptyList(), null)));

        // Test mismatch: pattern expects 2D but got 1D
        assertFalse(new MethodMatcher("com.example.Foo bar(String[][])").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(stringArray), emptyList(), emptyList(), emptyList(), null)));
    }

    @Test
    void varargsWithArrayElementType() {
        // Varargs with array element type: int[]...
        // In JavaType, this is represented as Array(Array(int))
        JavaType.Array intArray = new JavaType.Array(null, JavaType.Primitive.Int, null);
        JavaType.Array intArrayArray = new JavaType.Array(null, intArray, null);

        // Pattern int[]... should match Array(Array(int))
        assertTrue(new MethodMatcher("com.example.Foo bar(int[]...)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(intArrayArray), emptyList(), emptyList(), emptyList(), null)));

        // Pattern int[][] should also match the same type (varargs is just sugar for array)
        assertTrue(new MethodMatcher("com.example.Foo bar(int[][])").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(intArrayArray), emptyList(), emptyList(), emptyList(), null)));

        // Test with parameters before varargs: foo(int, int[]...)
        JavaType.Array stringArrayArray = new JavaType.Array(null,
          new JavaType.Array(null, JavaType.Primitive.String, null), null);
        assertTrue(new MethodMatcher("com.example.Foo bar(int, String[]...)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(JavaType.Primitive.Int, stringArrayArray),
            emptyList(), emptyList(), emptyList(), null)));
    }

    @Test
    void wildcardPatternsSkipChecks() throws Exception {
        // Test that patterns with wildcards for type and method name work correctly
        // This verifies that the check-skipping optimization doesn't break functionality

        // Pattern: * *(..) - matches any method on any type
        assertTrue(new MethodMatcher("* *(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        // Pattern: * foo(..) - matches foo method on any type
        assertTrue(new MethodMatcher("* foo(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "foo",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        assertFalse(new MethodMatcher("* foo(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        // Pattern: com.example.Foo *(..) - matches any method on com.example.Foo
        assertTrue(new MethodMatcher("com.example.Foo *(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        assertFalse(new MethodMatcher("com.example.Foo *(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Bar"), "bar",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        // Pattern: *..* *(..) - equivalent to * *(..), should skip both type and method checks
        assertTrue(new MethodMatcher("*..* *(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        // Pattern: *..* foo(..) - equivalent to * foo(..), should skip type check
        assertTrue(new MethodMatcher("*..* foo(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "foo",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        assertFalse(new MethodMatcher("*..* foo(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, emptyList(), emptyList(), emptyList(), emptyList(), null)));

        // Pattern: com.example.Foo foo(..) - should skip argument checks
        assertTrue(new MethodMatcher("com.example.Foo foo(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "foo",
            null, null, List.of(JavaType.Primitive.Int, JavaType.Primitive.String),
            emptyList(), emptyList(), emptyList(), null)));

        // Pattern: * *(..) - should skip all checks (method name, target type, arguments)
        assertTrue(new MethodMatcher("* *(..)").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "bar",
            null, null, List.of(JavaType.Primitive.Int),
            emptyList(), emptyList(), emptyList(), null)));

        // Pattern: com.example.Foo foo() - should check arg count (expects zero args)
        assertTrue(new MethodMatcher("com.example.Foo foo()").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "foo",
            null, null, emptyList(),
            emptyList(), emptyList(), emptyList(), null)));

        assertFalse(new MethodMatcher("com.example.Foo foo()").matches(
          new JavaType.Method(null, 1L, build("com.example.Foo"), "foo",
            null, null, List.of(JavaType.Primitive.Int),
            emptyList(), emptyList(), emptyList(), null)));
    }

    @Test
    void matcherWithNoArgsDoesNotMatchMethodWithArgs() {
        assertFalse(new MethodMatcher("java.lang.Object finalize()").matches(
          new JavaType.Method(null, 1L, build("my.Foo"), "finalize",
            null, null, List.of(build("java.lang.Object")),
            emptyList(), emptyList(), emptyList(), null)));
    }

    @Test
    void varargsMatcherValidatesVarargsArgumentTypes() {
        MethodMatcher matcher = new MethodMatcher("org.springframework.core.env.Environment acceptsProfiles(java.lang.String...)");

        JavaType.Method correctMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.core.env.Environment"),
          "acceptsProfiles",
          null, null,
          List.of(new JavaType.Array(null, build("java.lang.String"), null)),
          emptyList(), emptyList(), emptyList(), null
        );
        assertTrue(matcher.matches(correctMethod), "Should match method with String varargs");

        JavaType.Method zeroArgsMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.core.env.Environment"),
          "acceptsProfiles",
          null, null,
          emptyList(),
          emptyList(), emptyList(), emptyList(), null
        );
        assertTrue(matcher.matches(zeroArgsMethod), "Should match method with zero varargs (varargs can be empty)");

        JavaType.Method multipleArgsMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.core.env.Environment"),
          "acceptsProfiles",
          null, null,
          List.of(build("java.lang.String"), build("java.lang.String"), build("java.lang.String")),
          emptyList(), emptyList(), emptyList(), null
        );
        assertTrue(matcher.matches(multipleArgsMethod), "Should match method with multiple String arguments");

        JavaType.Method wrongTypeMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.core.env.Environment"),
          "acceptsProfiles",
          null, null,
          List.of(JavaType.Primitive.Int),
          emptyList(), emptyList(), emptyList(), null
        );
        assertFalse(matcher.matches(wrongTypeMethod), "Should NOT match method when varargs argument is wrong type (int instead of String)");

        JavaType.Method mixedArgsMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.core.env.Environment"),
          "acceptsProfiles",
          null, null,
          List.of(build("java.lang.String"), JavaType.Primitive.Int),
          emptyList(), emptyList(), emptyList(), null
        );
        assertFalse(matcher.matches(mixedArgsMethod), "Should NOT match method when second varargs argument is wrong type");
    }

    @Test
    void wildcardVarargsWithTrailingParameter() {
        // Pattern: get*Record*(.., long) means "any method starting with 'get' containing 'Record', any number of args, followed by long"
        MethodMatcher matcher = new MethodMatcher("org.springframework.kafka.test.utils.KafkaTestUtils get*Record*(.., long)");

        JavaType.Method correctMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.kafka.test.utils.KafkaTestUtils"),
          "getRecords",
          null, null,
          List.of(build("org.apache.kafka.clients.consumer.Consumer"), JavaType.Primitive.Long),
          emptyList(), emptyList(), emptyList(), null
        );
        assertTrue(matcher.matches(correctMethod), "Should match getRecords(Consumer, long)");

        JavaType.Method wrongLastParamMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.kafka.test.utils.KafkaTestUtils"),
          "getRecords",
          null, null,
          List.of(build("org.apache.kafka.clients.consumer.Consumer"), JavaType.Primitive.Int),
          emptyList(), emptyList(), emptyList(), null
        );
        assertFalse(matcher.matches(wrongLastParamMethod), "Should NOT match getRecords(Consumer, int) - last param must be long");

        JavaType.Method noArgsMethod = new JavaType.Method(
          null, 1L,
          build("org.springframework.kafka.test.utils.KafkaTestUtils"),
          "getRecords",
          null, null,
          List.of(JavaType.Primitive.Long),
          emptyList(), emptyList(), emptyList(), null
        );
        assertTrue(matcher.matches(noArgsMethod), "Should match getRecords(long) - wildcard varargs can match zero args");
    }
}
