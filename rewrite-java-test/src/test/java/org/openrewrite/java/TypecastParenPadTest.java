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
package org.openrewrite.java;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.cleanup.TypecastParenPad;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.style.TypecastParenPadStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.Collection;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
class TypecastParenPadTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TypecastParenPad());
    }

    private static Iterable<NamedStyles> namedStyles(Collection<Style> styles) {
        return singletonList(new NamedStyles(Tree.randomId(), "Test", "test", "test", emptySet(), styles));
    }

    @Test
    void addTypecastPadding() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(new TypecastParenPadStyle(true))))),
          java(
            """
              class Test {
                  static void method() {
                      long m = 0L;
                      int n = (int) m;
                      n = ( int) m;
                      n = (int ) m;
                      n = ( int ) m;
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      long m = 0L;
                      int n = ( int ) m;
                      n = ( int ) m;
                      n = ( int ) m;
                      n = ( int ) m;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void removeTypecastPadding() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(new TypecastParenPadStyle(false))))),
          java(
            """
              class Test {
                  static void method() {
                      long m = 0L;
                      int n = (int) m;
                      n = ( int) m;
                      n = (int ) m;
                      n = ( int ) m;
                  }
              }
              """,
            """
              class Test {
                  static void method() {
                      long m = 0L;
                      int n = (int) m;
                      n = (int) m;
                      n = (int) m;
                      n = (int) m;
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    private static Consumer<SourceSpec<J.CompilationUnit>> autoFormatIsIdempotent() {
        return spec -> spec.afterRecipe(cu ->
          Assertions.assertThat(new AutoFormatVisitor<>().visit(cu, 0)).isEqualTo(cu));
    }
}
