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
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.style.LineWrapSetting.*;

class WrapRecordComponentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        autoFormat(
          spaces -> spaces,
          wrapping -> wrapping.withRecordComponents(wrapping.getRecordComponents().withWrap(WrapAlways).withOpenNewLine(true))
        ).accept(spec);
    }

    private static Consumer<RecipeSpec> autoFormat(UnaryOperator<SpacesStyle> spaces,
                                                   UnaryOperator<WrappingAndBracesStyle> wrapping) {
        return spec -> spec.recipe(new AutoFormat(null))
          .parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              List.of(
                spaces.apply(IntelliJ.spaces()),
                wrapping.apply(IntelliJ.wrappingAndBraces().withKeepWhenFormatting(IntelliJ.wrappingAndBraces().getKeepWhenFormatting().withLineBreaks(false)))
              )
            )
          )));
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
                record Person(@Deprecated String name, int age) {
                }
                """,
              """
                record Person(
                        @Deprecated String name,
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
          autoFormat(
            spaces -> spaces,
            wrapping -> wrapping
              .withHardWrapAt(50)
              .withRecordComponents(wrapping.getRecordComponents().withWrap(ChopIfTooLong).withOpenNewLine(true))
          ),
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
          autoFormat(
            spaces -> spaces,
            wrapping -> wrapping.withRecordComponents(wrapping.getRecordComponents().withWrap(ChopIfTooLong))
          ),
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
          autoFormat(
            spaces -> spaces,
            wrapping -> wrapping.withRecordComponents(wrapping.getRecordComponents().withWrap(WrapAlways).withCloseNewLine(true))
          ),
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
