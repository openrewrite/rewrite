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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class InstanceOfTest implements RewriteTest {

    @Test
    void instanceOf() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(Object o) {
                      boolean b = o instanceof String;
                  }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void patternMatch() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class Test {
                public void match(Collection<?> c) {
                      if (c instanceof List<?> l) {
                          System.out.println("List");
                      } else if (c instanceof Set<?> s) {
                          System.out.println("Set");
                      }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaVisitor<Integer>() {
                @Override
                public J visitInstanceOf(J.InstanceOf instanceOf, Integer integer) {
                    assertThat(instanceOf.getPattern()).isNotNull();
                    return instanceOf;
                }
            }.visit(cu, 0))
          )
        );
    }

    @MinimumJava17
    @Test
    void instanceofPatternMatchWithFinalModifier() {
        rewriteRun(
          java(
            """
              public class Main {
                public static void main(String[] argv) {
                  String name = "Messi";
                  if (name instanceof final String player) {
                      System.out.println(player);
                  }
                }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void instanceofPatternMatchWithoutModifier() {
        rewriteRun(
          java(
            """
              public class Main {
                public static void main(String[] argv) {
                  String name = "Messi";
                  if (name instanceof String player) {
                      System.out.println(player);
                  }
                }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void instanceofPatternMatchWithNonTypeUseAnnotation() {
        // @SuppressWarnings has no TYPE_USE target, so the compiler stores it in the binding
        // variable's mods.annotations rather than in the type node (JCAnnotatedType). The
        // parser must still consume it from source before converting the type, otherwise the
        // cursor lands inside the annotation text and corrupts the resulting AST.
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                  void test(Object actual) {
                      if (actual instanceof @SuppressWarnings("rawtypes") List list) {
                          System.out.println(list.size());
                      }
                  }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void instanceofPatternMatchWithMultipleNonTypeUseAnnotations() {
        // Both @SuppressWarnings and @Deprecated lack TYPE_USE target, so both end up in
        // mods.annotations. collectAnnotations must consume them all before the type is converted.
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                  void test(Object actual) {
                      if (actual instanceof @SuppressWarnings("rawtypes") @Deprecated List list) {
                          System.out.println(list.size());
                      }
                  }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void instanceofPatternMatchWithFinalAndNonTypeUseAnnotation() {
        // Both the final-modifier path and the non-TYPE_USE annotation path must activate
        // without the cursor being mis-positioned between them.
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                  void test(Object actual) {
                      if (actual instanceof final @SuppressWarnings("rawtypes") List list) {
                          System.out.println(list.size());
                      }
                  }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void instanceofPatternMatchWithTypeUseAnnotation() {
        // In a binding pattern a leading annotation is parsed as a modifier of the binding
        // variable, so even a TYPE_USE annotation ends up in mods.annotations (not in a
        // JCAnnotatedType type node). It must be consumed from source before the type is converted.
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              class Test {
                  @Target(ElementType.TYPE_USE)
                  @interface NotNull {}

                  void test(Object obj) {
                      if (obj instanceof @NotNull String s) {
                          System.out.println(s);
                      }
                  }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void instanceofPatternMatchWithLeadingAndQualifiedTypeUseAnnotation() {
        // A leading non-TYPE_USE annotation (in mods.annotations) combined with a TYPE_USE
        // annotation in mid-qualified-name position (which makes node.getType() a JCAnnotatedType).
        // The leading annotation must still be consumed from source before converting the type.
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              class Test {
                  @Target(ElementType.TYPE_USE)
                  @interface NotNull {}

                  void test(Object actual) {
                      if (actual instanceof @SuppressWarnings("rawtypes") java.util.@NotNull List list) {
                          System.out.println(list.size());
                      }
                  }
              }
              """
          )
        );
    }
}
