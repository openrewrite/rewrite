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
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NormalizeTabsOrSpacesTest implements RewriteTest {

    @DocumentExample
    @Test
    void mixedToTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          kotlin(
            """
              class Test {
                  fun test() {
                      var n = 1
                  }
              }
              """,
            """
              class Test {
              	fun test() {
              		var n = 1
              	}
              }
              """
          )
        );
    }

    private static Consumer<RecipeSpec> tabsAndIndents() {
        return tabsAndIndents(style -> style);
    }

    private static Consumer<RecipeSpec> tabsAndIndents(UnaryOperator<org.openrewrite.kotlin.style.TabsAndIndentsStyle> with) {
        return spec -> spec.recipe(toRecipe(() -> new NormalizeTabsOrSpacesVisitor<>(with.apply(org.openrewrite.kotlin.style.IntelliJ.tabsAndIndents()))))
          .parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(org.openrewrite.kotlin.style.IntelliJ.tabsAndIndents()))
            )
          )));
    }

    @Test
    void mixedToSpaces() {
        rewriteRun(
          tabsAndIndents(),
          kotlin(
            """
              class Test {
                  fun test() {
              		val n = 1
                  }
              }
              """,
            """
              class Test {
                  fun test() {
                      val n = 1
                  }
              }
              """
          )
        );
    }

    @Test
    void tabsReplacedWithSpaces() {
        rewriteRun(
          tabsAndIndents(),
          kotlin(
            """
              class Test {
              	var a = 1
              	var b = 2
              	var c = 3
              	var d = 4

              	/*
              	 *
              	 *
              	 *
              	 *
              	 */

              	/**
              	 *
              	 *
              	 *
              	 *
              	 */
              	fun test() {
              	}
              }
              """,
            """
              class Test {
                  var a = 1
                  var b = 2
                  var c = 3
                  var d = 4

                  /*
                   *
                   *
                   *
                   *
                   */

                  /**
                   *
                   *
                   *
                   *
                   */
                  fun test() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/929")
    @Test
    void doNotReplaceSpacesBeforeAsterisks() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          kotlin(
            """
              /**
               *
               */
              class Test {
              	/*
              	 * Preserve `*`'s on
              	 * each new line.
              	 */
              	class Inner {
              	}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/928")
    @Test
    void normalizeJavaDocSuffix() {
        rewriteRun(
          tabsAndIndents(),
          kotlin(
            """
              class Test {
              	/** Test
              	 */
              	class Inner {
              	}
              }
              """,
            """
              class Test {
                  /** Test
                   */
                  class Inner {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1237")
    @Test
    void normalizeLastWhitespace() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          kotlin(
            """
              class Test {
              	fun test() {
              		var n = 1
               }
              }
              """,
            """
              class Test {
              	fun test() {
              		var n = 1
              	}
              }
              """
          )
        );
    }
}
