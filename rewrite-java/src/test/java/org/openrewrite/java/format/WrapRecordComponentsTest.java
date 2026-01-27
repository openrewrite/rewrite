/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.style.LineWrapSetting.*;
import static org.openrewrite.style.StyleHelper.fromStyles;
import static org.openrewrite.test.RewriteTest.toRecipe;

class WrapRecordComponentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(
          List.of(
            fromStyles(
              new WrappingAndBracesStyle(
                120,
                new WrappingAndBracesStyle.IfStatement(false),
                new WrappingAndBracesStyle.ChainedMethodCalls(DoNotWrap, Arrays.asList("builder", "newBuilder"), false),
                new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
                new WrappingAndBracesStyle.MethodCallArguments(DoNotWrap, false, false, false),
                new WrappingAndBracesStyle.RecordComponents(WrapAlways, false, true, false),
                null,
                null,
                null,
                null,
                null,
                null))),
          null)));
    }

    @DocumentExample
    @Test
    void formatRecordWithMultipleComponents() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                record Person(String name, int age, boolean active) {
                }
                """,
              """
                record Person(
                String name,
                int age,
                boolean active) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void formatRecordWithTwoComponents() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                record Point(int x, int y) {
                }
                """,
              """
                record Point(
                int x,
                int y) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void preserveAlreadyFormattedRecord() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                record Person(
                        String name,
                        int age,
                        boolean active) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void formatRecordWithGenericTypes() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.List;
                import java.util.Map;

                record Container(List<String> names, Map<String, Integer> ages) {
                }
                """,
              """
                import java.util.List;
                import java.util.Map;

                record Container(
                List<String> names,
                Map<String, Integer> ages) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void formatRecordWithAnnotatedComponents() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import org.jspecify.annotations.Nullable;

                record Person(@Nullable String name, int age) {
                }
                """,
              """
                import org.jspecify.annotations.Nullable;

                record Person(
                @Nullable String name,
                int age) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void formatLongLinesOnly() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(
            List.of(
              fromStyles(
                new WrappingAndBracesStyle(
                  50,
                  new WrappingAndBracesStyle.IfStatement(false),
                  new WrappingAndBracesStyle.ChainedMethodCalls(DoNotWrap, List.of(), false),
                  new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
                  new WrappingAndBracesStyle.MethodCallArguments(DoNotWrap, false, false, false),
                  new WrappingAndBracesStyle.RecordComponents(ChopIfTooLong, false, true, false),
                  null,
                  null,
                  null,
                  null,
                  null,
                  null))),
            null))),
          version(
            java(
              """
                record Short(int x, int y) {
                }

                record VeryLongRecordNameThatExceedsTheLimit(String name, int age, boolean active) {
                }
                """,
              """
                record Short(int x, int y) {
                }

                record VeryLongRecordNameThatExceedsTheLimit(
                String name,
                int age,
                boolean active) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void preserveRecordWithLengthBelowThreshold() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(
            List.of(
              fromStyles(
                new WrappingAndBracesStyle(
                  120,
                  new WrappingAndBracesStyle.IfStatement(false),
                  new WrappingAndBracesStyle.ChainedMethodCalls(DoNotWrap, List.of(), false),
                  new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
                  new WrappingAndBracesStyle.MethodCallArguments(DoNotWrap, false, false, false),
                  new WrappingAndBracesStyle.RecordComponents(ChopIfTooLong, false, false, false),
                  null,
                  null,
                  null,
                  null,
                  null,
                  null))),
            null))),
          version(
            java(
              """
                record Point(int x, int y) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void openCloseNewLine() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(
            List.of(
              fromStyles(
                new WrappingAndBracesStyle(
                  120,
                  new WrappingAndBracesStyle.IfStatement(false),
                  new WrappingAndBracesStyle.ChainedMethodCalls(DoNotWrap, List.of(), false),
                  new WrappingAndBracesStyle.MethodDeclarationParameters(DoNotWrap, false, false, false),
                  new WrappingAndBracesStyle.MethodCallArguments(DoNotWrap, false, false, false),
                  new WrappingAndBracesStyle.RecordComponents(WrapAlways, false, false, true),
                  null,
                  null,
                  null,
                  null,
                  null,
                  null))),
            null))),
          version(
            java(
              """
                record Person(String name, int age, boolean active) {
                }
                """,
              """
                record Person(String name,
                int age,
                boolean active
                ) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void doNotFormatComponentsOnTheirOwnNewLineAlready() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                record Person(
                    String name,
                    int age) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void doNotWrapSingleComponentRecord() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                record Wrapper(String value) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void doNotWrapNoComponentRecord() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                record Empty() {
                }
                """
            ),
            17
          )
        );
    }
}
