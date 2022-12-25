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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class NormalizeTabsOrSpacesTest implements RewriteTest {

    private static Consumer<RecipeSpec> tabsAndIndents() {
        return tabsAndIndents(style -> style);
    }

    private static Consumer<RecipeSpec> tabsAndIndents(UnaryOperator<TabsAndIndentsStyle> with) {
        return spec -> spec.recipe(new NormalizeTabsOrSpaces())
          .parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.tabsAndIndents()))
            )
          )));
    }

    @Test
    void mixedToTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          java(
            """
              public class Test {
                  public void test() {
                      int n = 1;
                  }
              }
              """,
            """
              public class Test {
              	public void test() {
              		int n = 1;
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
          java(
            """
              public class Test {
                  public void test() {
              		int n = 1;
                  }
              }
              """,
            """
              public class Test {
                  public void test() {
                      int n = 1;
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
          java(
            """
              public class Test {
              	int a;
              	int b;
              	int c;
              	int d;
              
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
              	void test() {
              	}
              }
              """,
            """
              public class Test {
                  int a;
                  int b;
                  int c;
                  int d;
              
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
                  void test() {
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
          java(
            """
              /**
               *
               */
              public class Test {
              	/*
              	 * Preserve `*`'s on
              	 * each new line.
              	 */
              	public class Inner {
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
          java(
            """
              public class Test {
              	/** Test
              	 */
              	public class Inner {
              	}
              }
              """,
            """
              public class Test {
                  /** Test
                   */
                  public class Inner {
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
          java(
            """
              public class Test {
              	public void test() {
              		int n = 1;
               }
              }
              """,
            """
              public class Test {
              	public void test() {
              		int n = 1;
              	}
              }
              """
          )
        );
    }
}
