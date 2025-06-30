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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.style.OtherStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class TrailingCommaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new TrailingCommaVisitor<>(IntelliJ.other().getUseTrailingComma())));
    }

    @DocumentExample
    @Test
    void classPropertiesWithTrailingCommaOff() {
        rewriteRun(
          trailingCommaStyle(style -> style.withUseTrailingComma(false)),
          kotlin(
            """
              class A(
                      val a: Boolean,
                      val b: Boolean,
              ) {}
              """,
            """
              class A(
                      val a: Boolean,
                      val b: Boolean
              ) {}
              """
          )
        );
    }

    private static Consumer<RecipeSpec> trailingCommaStyle(UnaryOperator<OtherStyle> with) {
        return spec -> spec.recipe(toRecipe(() -> new TrailingCommaVisitor<>(with.apply(IntelliJ.other()).getUseTrailingComma())));
    }

    @Test
    void classPropertiesWithTrailingCommaOn() {
        rewriteRun(
          trailingCommaStyle(style -> style.withUseTrailingComma(true)),
          kotlin(
            """
              class A(
                      val a: Boolean,
                      val b: Boolean
              ) {}
              """,
            """
              class A(
                      val a: Boolean,
                      val b: Boolean,
              ) {}
              """
          )
        );
    }

    @Test
    void methodWithTrailingCommaOff() {
        rewriteRun(
          trailingCommaStyle(style -> style.withUseTrailingComma(false)),
          kotlin(
            """
              fun method(arg1: String,
                         arg2: String,
              ) {}

              val x = method(
                  "foo",
                  "bar",
              )

              val y = method(
                  "x",
                  if (true) "foo" else "bar",
              )
              """,
            """
              fun method(arg1: String,
                         arg2: String
              ) {}

              val x = method(
                  "foo",
                  "bar"
              )

              val y = method(
                  "x",
                  if (true) "foo" else "bar"
              )
              """
          )
        );
    }

    @Test
    void methodWithTrailingCommaOn() {
        rewriteRun(
          trailingCommaStyle(style -> style.withUseTrailingComma(true)),
          kotlin(
            """
              fun method(arg1: String,
                         arg2: String
              ) {}

              val x = method(
                  "foo",
                  "bar"
              )

              val y = method(
                  "x",
                  if (true) "foo" else "bar"
              )
              """,
            """
              fun method(arg1: String,
                         arg2: String,
              ) {}

              val x = method(
                  "foo",
                  "bar",
              )

              val y = method(
                  "x",
                  if (true) "foo" else "bar",
              )
              """
          )
        );
    }
}
