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

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.cleanup.PadEmptyForLoopComponents;
import org.openrewrite.java.format.SpacesVisitor;
import org.openrewrite.java.style.EmptyForInitializerPadStyle;
import org.openrewrite.java.style.EmptyForIteratorPadStyle;
import org.openrewrite.java.style.IntelliJ;
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

/**
 * It's important that these tests validate both that PadEmptyForLoopComponents does the right thing and that
 * SpacesVisitor does not undo that change. Since AutoFormat is used frequently if there were a disagreement over
 * this formatting they would fight back and forth until max cycles was reached.
 */
@SuppressWarnings("ClassInitializerMayBeStatic")
class PadEmptyForLoopComponentsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PadEmptyForLoopComponents());
    }

    private static Iterable<NamedStyles> namedStyles(Collection<Style> styles) {
        return singletonList(new NamedStyles(Tree.randomId(), "Test", "test", "test", emptySet(), styles));
    }

    @Test
    void addSpaceToEmptyInitializer() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(new EmptyForInitializerPadStyle(true))))),
          java(
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for (; i < j; i++, j--) { }
                  }
              }
              """,
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for ( ; i < j; i++, j--) { }
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void removeSpaceFromEmptyInitializer() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(new EmptyForInitializerPadStyle(false))))),
          java(
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for ( ; i < j; i++, j--) { }
                  }
              }
              """,
            """
              public class A {
                  {
                      int i = 0;
                      int j = 10;
                      for (; i < j; i++, j--) { }
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void addSpaceToEmptyIterator() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(new EmptyForInitializerPadStyle(true))))),
          java(
            """
              public class A {
                  {
                      int i = 0;
                      for (int i = 0; i < 10;) { i++; }
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void removeSpaceFromEmptyIterator() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(new EmptyForInitializerPadStyle(false))))),
          java(
            """
              public class A {
                  {
                      int i = 0;
                      for (int i = 0; i < 10; ) { i++; }
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    private static Consumer<SourceSpec<J.CompilationUnit>> autoFormatIsIdempotent() {
        return spec -> spec.afterRecipe(cu ->
          org.assertj.core.api.Assertions.assertThat(new SpacesVisitor<>(IntelliJ.spaces(), null,
            new EmptyForIteratorPadStyle(false)).visit(cu, 0)).isEqualTo(cu));
    }
}
