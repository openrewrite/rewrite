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
package org.openrewrite.json.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.style.LineWrapSetting;
import org.openrewrite.style.Style;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;

class AutodetectTest implements RewriteTest {

    private static final String sgml =
      """
        {
          "glossary": {
            "title": "example glossary",
            "GlossDiv": {
              "title": "S",
              "GlossList": {
                "GlossEntry": {
                  "ID": "SGML",
                  "SortAs": "SGML",
                  "GlossTerm": "Standard Generalized Markup Language",
                  "Acronym": "SGML",
                  "Abbrev": "ISO 8879:1986",
                  "GlossDef": {
                    "para": "A meta-markup language, used to create markup languages such as DocBook.",
                    "GlossSeeAlso": [
                      "GML",
                      "XML"
                    ]
                  },
                  "GlossSee": "markup"
                }
              }
            }
          }
        }
        """;

    @Test
    void autodetectSimple() {
        rewriteRun(
          withDetectedIndentation(tabsAndIndents -> {
              assertThat(tabsAndIndents.getTabSize()).isEqualTo(1);
              assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
              assertThat(tabsAndIndents.getUseTabCharacter()).isEqualTo(false);
          }),
          json(sgml)
        );
    }

    @Test
    void tabs() {
        rewriteRun(
          withDetectedIndentation(tabsAndIndents ->
            assertThat(tabsAndIndents.getUseTabCharacter()).isEqualTo(true)),
          json(
            """
              {
              TAB"name": "John",
              TAB"age": 30,
              TAB"car": null
              }
              """.replaceAll("TAB", "\t")
          )
        );
    }

    @Test
    void noNewLines() {
        rewriteRun(
          withDetectedWrappingAndBraces(wrappingAndBraces -> {
              assertThat(wrappingAndBraces.getWrapArrays()).isEqualTo(LineWrapSetting.DoNotWrap);
              assertThat(wrappingAndBraces.getWrapObjects()).isEqualTo(LineWrapSetting.DoNotWrap);
          }),
          json(
            """
              {"pl":[{"pid":"2544","fn":"LeBron","ln":"James","val":"38","tid":1610612747,"ta":"LAL","tn":"Lakers","tc":"Los Angeles"}]}
              """
          )
        );
    }

    @Test
    void fullWrapping() {
        rewriteRun(
          withDetectedWrappingAndBraces(wrappingAndBraces -> {
              assertThat(wrappingAndBraces.getWrapArrays()).isEqualTo(LineWrapSetting.WrapAlways);
              assertThat(wrappingAndBraces.getWrapObjects()).isEqualTo(LineWrapSetting.WrapAlways);
          }),
          json(sgml)
        );
    }

    @Test
    void wrapObjectsButNotArrays() {
        rewriteRun(
          withDetectedWrappingAndBraces(wrappingAndBraces -> {
              assertThat(wrappingAndBraces.getWrapArrays()).isEqualTo(LineWrapSetting.DoNotWrap);
              assertThat(wrappingAndBraces.getWrapObjects()).isEqualTo(LineWrapSetting.WrapAlways);
          }),
          json(
            """
              {
                  "x": "x",
                  "l": [1, 2],
                  "key": {
                      "a": "b"
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyObjectsAlsoCounted() {
        rewriteRun(
          withDetectedWrappingAndBraces(wrappingAndBraces ->
            assertThat(wrappingAndBraces.getWrapObjects()).isEqualTo(LineWrapSetting.WrapAlways)),
          json(
            //(outer object + 3 members + 1 empty object + 1 nested + 1 nested empty value) 7 == 7 (3 members + 1 empty, 1 empty with space + 1 nested + 1 nested empty value)
            """
              { "non-wrapped-empty": {}, "non-wrapped-empty-space": { }, "non-wrapped": { "nested": {} },
                "wrapped-empty": {
                },
                "wrapped-empty-line": {
              
                },
                "wrapped": {
                  "nested": {
                  }
                }
              }
              """
          )
        );
    }

    private static Consumer<RecipeSpec> withDetectedIndentation(Consumer<TabsAndIndentsStyle> fn) {
        return withDetectedStyle(TabsAndIndentsStyle.class, fn);
    }

    private static Consumer<RecipeSpec> withDetectedWrappingAndBraces(Consumer<WrappingAndBracesStyle> fn) {
        return withDetectedStyle(WrappingAndBracesStyle.class, fn);
    }

    private static <S extends Style> Consumer<RecipeSpec> withDetectedStyle(Class<S> styleClass, Consumer<S> fn) {
        return spec -> spec.beforeRecipe(sources -> {
            Autodetect.Detector detector = Autodetect.detector();
            sources.forEach(detector::sample);

            @SuppressWarnings("unchecked")
            var foundStyle = (S) detector.build().getStyles().stream()
              .filter(styleClass::isInstance)
              .findAny().orElseThrow();
            fn.accept(foundStyle);
        });
    }
}
