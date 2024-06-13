/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.groovy.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.format.NormalizeTabsOrSpacesVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

class NormalizeTabsOrSpacesTest implements RewriteTest {
    private static Consumer<RecipeSpec> tabsAndIndents() {
        return tabsAndIndents(style -> style);
    }

    private static Consumer<RecipeSpec> tabsAndIndents(UnaryOperator<TabsAndIndentsStyle> with) {
        return spec -> spec.recipe(toRecipe(() -> new NormalizeTabsOrSpacesVisitor<>(with.apply(IntelliJ.tabsAndIndents()))))
          .parser(GroovyParser.builder().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.tabsAndIndents()))
            )
          )));
    }

    @DocumentExample
    @Test
    void mixedToTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          groovy(
            """
              class Test {
                  def test() {
                      var n = 1
                  }
              }
              """,
            """
              class Test {
              	def test() {
              		var n = 1
              	}
              }
              """
          )
        );
    }

    @Test
    void mixedToSpaces() {
        rewriteRun(
          tabsAndIndents(),
          groovy(
            """
              class Test {
                  def test() {
              		val n = 1
                  }
              }
              """,
            """
              class Test {
                  def test() {
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
          groovy(
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
              	def test() {
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
                  def test() {
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
          groovy(
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
          groovy(
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
          groovy(
            """
              class Test {
              	def test() {
              		var n = 1
               }
              }
              """,
            """
              class Test {
              	def test() {
              		var n = 1
              	}
              }
              """
          )
        );
    }
}
