/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.style;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.python;

class AutodetectTest implements RewriteTest {

    @Test
    void autodetectSpaces2() {
        rewriteRun(
          hasIndentation(2),
          python(
                """
          if 1 > 0:
           print("This one-space indent will be effectively ignored")

          for i in range(1, 24):
            print(i)
            for j in range(1, i):
              x = j * j
              print(i, j)
          """
          )
        );
    }

    @Test
    void autodetectSpaces4() {
        rewriteRun(
          hasIndentation(4),
          python(
                """
          if 1 > 0:
           print("This one-space indent will be effectively ignored")

          for i in range(1, 24):
              print(i)
              for j in range(1, i):
                  x = j * j
                  print(i, j)
          """
          )
        );
    }

    private static Consumer<RecipeSpec> hasSpacingStyle(Consumer<SpacesStyle> assertions) {
        return spec -> spec.beforeRecipe(sources -> {
            Autodetect.Detector detector = Autodetect.detector();
            sources.forEach(detector::sample);

            SpacesStyle spaces = (SpacesStyle) detector.build().getStyles().stream()
              .filter(SpacesStyle.class::isInstance)
              .findAny().orElseThrow();
            assertions.accept(spaces);
        });
    }

    @Test
    void autodetectOperatorSpacing() {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getAroundOperators().getAssignment()).isTrue();
              assertThat(style.getAroundOperators().getAdditive()).isTrue();
              assertThat(style.getAroundOperators().getMultiplicative()).isTrue();
          }),
          python(
                """
            x = 1 + 2
            y = 3 * 4
            z = x + y
            result = (x + y) * z
            """
          )
        );
    }

    @Test
    void autodetectNoOperatorSpacing() {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getAroundOperators().getAssignment()).isFalse();
              assertThat(style.getAroundOperators().getAdditive()).isFalse();
              assertThat(style.getAroundOperators().getMultiplicative()).isFalse();
          }),
          python(
                """
            x=1+2
            y=3*4
            z=x+y
            result=(x+y)*z
            """
          )
        );
    }

    @Test
    void autodetectMethodSpacing() {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getBeforeParentheses().getMethodCall()).isTrue();
              assertThat(style.getWithin().getMethodCallParentheses()).isTrue();
          }),
          python(
                """
            def test ( x, y ):
                return x + y
            
            result = test ( 1, 2 )
            """
          )
        );
    }

    @Test
    void autodetectTypeHintSpacing() {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getOther().getBeforeColon()).isTrue();
              assertThat(style.getOther().getAfterColon()).isTrue();
          }),
          python(
                """
            def calculate(x : int, y : float) -> float:
                return x + y
            
            result : float = calculate(1, 2.0)
            """
          )
        );
    }

    @CsvSource({
      "True, False",
      "False, True"
    })
    @ParameterizedTest
    void autodetectCommaSpacing(Boolean spaceAfterComma, Boolean spaceBeforeComma) {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getOther().getBeforeComma()).isEqualTo(spaceBeforeComma);
              assertThat(style.getOther().getAfterComma()).isEqualTo(spaceAfterComma);
          }),
          python("""
            numbers = [1, 2, 3, 4, 5]
            data = {"a": 1, "b": 2, "c": 3}
            def test(x, y, z):
                pass
            """.replace(", ", spaceAfterComma ? ", " : ",").replace(",", spaceBeforeComma ? " ," : ","))
        );
    }

    @Test
    void autodetectBraceAndBracketSpacing() {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getWithin().getBrackets()).isTrue();
              assertThat(style.getWithin().getBraces()).isTrue();
          }),
          python(
                """
            numbers = [ 1, 2, 3 ]
            data = { "a": 1, "b": 2 }
            matrix = [ [ 1, 2 ], [ 3, 4 ] ]
            """
          )
        );
    }

    @Test
    void autodetectBraceAndBracketSpacingEmpty() {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getWithin().getBrackets()).isTrue();
              assertThat(style.getWithin().getBraces()).isTrue();
          }),
          python(
                """
            numbers = [  ]
            data = {  }
            matrix = [ [  ], [  ] ]
            """
          )
        );
    }

    @Test
    void autodetectComprehensionSpacing() {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getWithin().getBrackets()).isTrue();
              assertThat(style.getWithin().getBraces()).isTrue();
          }),
          python(
                """
            squares = [ x*x for x in range(10) ]
            evens = { x for x in range(20) if x % 2 == 0 }
            """
          )
        );
    }

    @CsvSource({
      "true, x = y = z = 1",
      "false, x=y=z=1"
    })
    @ParameterizedTest(name = "Chained Assignment - with spaces={0}")
    void autodetectChainedAssignmentSpacing(boolean hasSpaces, String code) {
        rewriteRun(
          hasSpacingStyle(style ->
              assertThat(style.getAroundOperators().getAssignment())
                .as("Assignment operators in chained assignments should%s have spaces", hasSpaces ? "" : " not")
                .isEqualTo(hasSpaces)),
          python("""
            %s
            %s
            """.formatted(code, code.replace("1", "x + y")))
        );
    }

    @CsvSource({
      "true,x += 1",
      "true,x -= 1",
      "true,x = 1",
      "true,x *= 1",
      "false,x+=1",
      "false,x-=1",
      "false,x=1",
      "false,x*=1",
    })
    @ParameterizedTest(name = "Assignment Operations - with spaces={0}")
    void autodetectAssignmentOperationSpacing(boolean hasSpaces, String operation) {
        rewriteRun(
          hasSpacingStyle(style ->
              assertThat(style.getAroundOperators().getAssignment())
                .as("Compound assignment operators should%s have spaces", hasSpaces ? "" : " not")
                .isEqualTo(hasSpaces)),
          python("""
            %s
            """.formatted(
            operation
          ))
        );
    }

    @CsvSource({
      "true, 'x = 1,y = 3'",
      "false, 'x=1, y=3'"
    })
    @Disabled("Not yet implemented")
    @ParameterizedTest(name = "Named Parameters - with spaces={0}")
    void autodetectNamedParameterSpacing(boolean hasSpaces, String params) {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getAroundOperators().getEqInNamedParameter())
                .as("Named parameter definitions should%s have spaces around equals", hasSpaces ? "" : " not")
                .isEqualTo(hasSpaces);
              assertThat(style.getAroundOperators().getEqInKeywordArgument())
                .as("Keyword arguments in function calls should%s have spaces around equals", hasSpaces ? "" : " not")
                .isEqualTo(hasSpaces);
          }),
          python("""
            def test(%s):
                return x + y
            
            result = test(%s)
            """.formatted(params, params))
        );
    }

    @CsvSource({
      "true",
      "false"
    })
    @ParameterizedTest(name = "Mixed Operators - with spaces={0}")
    void autodetectMixedOperatorSpacing(boolean hasSpaces) {
        rewriteRun(
          hasSpacingStyle(style -> {
              assertThat(style.getAroundOperators().getPower())
                .as("Power operators should%s have spaces", hasSpaces ? "" : " not")
                .isEqualTo(hasSpaces);
              assertThat(style.getAroundOperators().getMultiplicative())
                .as("Floor division and matrix multiplication operators should%s have spaces", hasSpaces ? "" : " not")
                .isEqualTo(hasSpaces);
          }),
          python("""
            x = 2 ** 3
            x = 2 * 3
            x = 2 // 3
            """.replace("2 ", hasSpaces ? "2 " : "2").replace(" 3", hasSpaces ? " 3" : "3"))
        );
    }

    @CsvSource({
      "true",
      "false"
    })
    @ParameterizedTest(name = "For Loop Semi-colon - with spaces={0}")
    void autodetectForLoop(boolean hasSpaces) {
        rewriteRun(
          hasSpacingStyle(style ->
              assertThat(style.getOther().getBeforeColon())
                .isEqualTo(hasSpaces)),
          python("""
            for i in range(10)%s:
                pass
            """.formatted(hasSpaces ? " " : ""))
        );
    }

    private static Consumer<RecipeSpec> hasIndentation(int indentSize) {
        return spec -> spec.beforeRecipe(sources -> {
            Autodetect.Detector detector = Autodetect.detector();
            sources.forEach(detector::sample);

            TabsAndIndentsStyle tabsAndIndents = (TabsAndIndentsStyle) detector.build().getStyles().stream()
              .filter(TabsAndIndentsStyle.class::isInstance)
              .findAny().orElseThrow();
            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(indentSize);
        });
    }
}
