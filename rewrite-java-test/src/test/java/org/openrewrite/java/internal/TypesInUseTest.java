/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantConditions")
class TypesInUseTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/617")
    @Test
    void findAnnotationArgumentType() {
        rewriteRun(
          java(
            """
              package org.openrewrite.test;

              public @interface YesOrNo {
                  Status status();
                  enum Status {
                      YES, NO
                  }
              }
              """
          ),
          java(
            """
              package org.openrewrite.test;

              import static org.openrewrite.test.YesOrNo.Status.YES;

              @YesOrNo(status = YES)
              public class Foo {}
              """,
            spec -> spec.afterRecipe(cu -> {
                var foundTypes = cu.getTypesInUse().getVariables().stream()
                  .map(v -> TypeUtils.asFullyQualified(v.getType()).getFullyQualifiedName())
                  .toList();
                assertThat(foundTypes).containsExactlyInAnyOrder("org.openrewrite.test.YesOrNo$Status");
            })
          )
        );
    }

    @Test
    void recordsFullyQualifiedJavadocReferencesSeparately() {
        rewriteRun(
          java(
            """
              package org.openrewrite.other;
              public class Target {}
              """
          ),
          java(
            """
              package com.example;
              /**
               * See {@link org.openrewrite.other.Target} for details.
               */
              public class Bar {}
              """,
            spec -> spec.afterRecipe(cu -> {
                TypesInUse tiu = cu.getTypesInUse();

                // Fully qualified Javadoc references stay out of the import-retention set (#5738)...
                assertThat(tiu.getTypesInUse().stream()
                  .filter(t -> t instanceof JavaType.FullyQualified)
                  .map(t -> ((JavaType.FullyQualified) t).getFullyQualifiedName()))
                  .doesNotContain("org.openrewrite.other.Target");

                // ...but their packages are recorded separately so package-renaming recipes can find them.
                assertThat(tiu.hasDocReferenceInPackage("org.openrewrite.other", false)).isTrue();
                assertThat(tiu.hasDocReferenceInPackage("org.openrewrite", false)).isFalse();
                assertThat(tiu.hasDocReferenceInPackage("org.openrewrite", true)).isTrue();
                assertThat(tiu.hasDocReferenceInPackage("com.other", true)).isFalse();
            })
          )
        );
    }

    @Test
    void methodMatchingAcrossPatternShapes() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              import java.util.List;
              class A {
                  void f(List<String> l) {
                      l.add("x");
                      l.get(0);
                      Collections.emptyList();
                      "a".concat("b");
                  }
                  int g() {
                      return 1;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                TypesInUse tiu = cu.getTypesInUse();

                // Exact method name on a concrete type.
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.util.List add(..)"))).isTrue();
                // Wildcard method name on a concrete type — the case the declaring-type prefix index must cover.
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.util.List *(..)"))).isTrue();
                // Subpackage prefix matches both List and Collections.
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.util..* *(..)"))).isTrue();
                // Mid-string type wildcard yields the weak "java." prefix but still matches.
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.*.List add(..)"))).isTrue();
                // Full type wildcard with a pinned name falls back to a full scan.
                assertThat(tiu.hasMethodUse(new MethodMatcher("*..* concat(..)"))).isTrue();

                // Negative: declaring type is referenced, but the method is not used.
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.util.List remove(..)"))).isFalse();
                // Negative: declaring type is not referenced at all (sorts past every key).
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.util.Map put(..)"))).isFalse();

                // matchOverrides bypasses the prefix index: List.add matches the Collection#add supertype pattern.
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.util.Collection add(..)", true))).isTrue();
                assertThat(tiu.hasMethodUse(new MethodMatcher("java.util.Collection add(..)", false))).isFalse();

                // Declared methods go through the same path.
                assertThat(tiu.declaresMethod(new MethodMatcher("A g()"))).isTrue();
                assertThat(tiu.declaresMethod(new MethodMatcher("A *(..)"))).isTrue();
                assertThat(tiu.declaresMethod(new MethodMatcher("A nonexistent()"))).isFalse();
            })
          )
        );
    }

    @Test
    void publicFactoryReturnsInstanceWithSuppliedSets() {
        rewriteRun(
          java(
            """
              package org.openrewrite.test;
              public class Foo {}
              """,
            spec -> spec.afterRecipe(cu -> {
                Set<JavaType> types = Collections.emptySet();
                Set<JavaType.Method> declaredMethods = Collections.emptySet();
                Set<JavaType.Method> usedMethods = Collections.emptySet();
                Set<JavaType.Variable> variables = Collections.emptySet();

                TypesInUse tiu = TypesInUse.of(cu, types, declaredMethods, usedMethods, variables);

                assertThat(tiu.getCu()).isSameAs(cu);
                assertThat(tiu.getTypesInUse()).isSameAs(types);
                assertThat(tiu.getDeclaredMethods()).isSameAs(declaredMethods);
                assertThat(tiu.getUsedMethods()).isSameAs(usedMethods);
                assertThat(tiu.getVariables()).isSameAs(variables);
            })
          )
        );
    }

}
