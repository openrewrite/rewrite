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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;

public class AutodetectTest implements RewriteTest {

    @Test
    void autodetectSimple() {
        rewriteRun(
          withDetectedIndentation(tabsAndIndents -> {
            assertThat(tabsAndIndents.getTabSize()).isEqualTo(1);
            assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
            assertThat(tabsAndIndents.getUseTabCharacter()).isEqualTo(false);
            return null;
          }),
          json(
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
              """
          )
        );
    }

    @Test
    void tabs() {
        rewriteRun(
          withDetectedIndentation( tabsAndIndents -> {
                assertThat(tabsAndIndents.getUseTabCharacter()).isEqualTo(true);
                return null;
            }),
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


    private static Consumer<RecipeSpec> withDetectedIndentation(Function<TabsAndIndentsStyle, Void> fn) {
        return spec -> spec.beforeRecipe(sources -> {
            Autodetect.Detector detector = Autodetect.detector();
            sources.forEach(detector::sample);

            TabsAndIndentsStyle tabsAndIndents = (TabsAndIndentsStyle) detector.build().getStyles().stream()
              .filter(TabsAndIndentsStyle.class::isInstance)
              .findAny().orElseThrow();
            fn.apply(tabsAndIndents);
        });
    }
}
