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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

@SuppressWarnings("CheckTagEmptyBody")
class SemanticallyEqualTest implements RewriteTest {

    @Test
    void attributeOrderDoesntAffectSemanticEquality() {
        rewriteRun(
          semanticallyEqual(true),
          xml("<foo fizz='fizz' buzz=\"buzz\"></foo>"),
          xml("<foo buzz=\"buzz\" fizz=\"fizz\"></foo>")
        );
    }

    @Test
    void tagContentsAreConsidered() {
        rewriteRun(
          semanticallyEqual(false),
          xml("<foo>foo</foo>"),
          xml("<foo>bar</foo>")
        );
    }

    @Test
    void attributesWithDifferentValuesAreNotEqual() {
        rewriteRun(
          semanticallyEqual(false),
          xml("<foo fizz='fizz' buzz=\"bang\"></foo>"),
          xml("<foo fizz=\"fizz\" buzz=\"buzz\" ></foo>")
        );
    }

    @Test
    void selfClosingTagIsEquivalentToEmptyTag() {
        rewriteRun(
          semanticallyEqual(true),
          xml("<foo></foo>"),
          xml("<foo/>")
        );
    }

    @Test
    void nestedTagsAreConsideredForEqualityAndFormattingDoesntMatter() {
        rewriteRun(
          semanticallyEqual(true),
          xml(
            """
              <foo>
                  <bar>
                      <baz>hello</baz>
                      <bing/>
                  </bar>
              </foo>
              """
          ),
          xml(
            """
              <foo><bar>
                      <baz>hello</baz>
                      <bing/>
                  </bar></foo>
              """
          )
        );
    }

    @Test
    void commentsInOrAroundTagsDontMatter() {
        rewriteRun(
          semanticallyEqual(true),
          xml(
            """
              <foo>
                  <bar>bing</bar>
              </foo>
              """
          ),
          xml(
            """
              <!-- foo -->
              <foo>
                  <!-- bar -->
                  <bar><!-- bing -->bing<!-- bing --></bar>
              </foo>
              """
          )
        );
    }

    private static Consumer<RecipeSpec> semanticallyEqual(boolean isEqual) {
        return spec -> spec.beforeRecipe(sources ->
          assertThat(SemanticallyEqual.areEqual((Xml) sources.get(0), (Xml) sources.get(1))).isEqualTo(isEqual));
    }
}
