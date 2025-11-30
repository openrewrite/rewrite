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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class PlainTextAsXmlTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PlainTextAsXml("**/*.cxms"));
    }

    @DocumentExample
    @Test
    void parsePlainTextAsXmlThenApplyXmlRecipe() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsXml("**/*.cxms"),
            new ChangeTagName("/root/child", "newChild")
          ),
          text(
            """
              <root>
                  <child>value</child>
              </root>
              """,
            """
              <root>
                  <newChild>value</newChild>
              </root>
              """,
            spec -> spec.path("test.cxms")
          )
        );
    }

    @Test
    void convertsToXmlDocument() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> {
              SourceFile result = run.getChangeset().getAllResults().getFirst().getAfter();
              assertThat(result).isInstanceOf(Xml.Document.class);
          }).recipes(
            new PlainTextAsXml("**/*.cxms"),
            new ChangeTagValue("/root/child", null, "newValue", null)
          ),
          text(
            """
              <root>
                  <child>value</child>
              </root>
              """,
            """
              <root>
                  <child>newValue</child>
              </root>
              """,
            spec -> spec.path("test.cxms")
          )
        );
    }

    @Test
    void doesNotMatchWhenPatternDoesNotMatch() {
        rewriteRun(
          text(
            """
              <root>
                  <child>value</child>
              </root>
              """,
            spec -> spec.path("test.other")
          )
        );
    }

    @Test
    void changeTagValue() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsXml("**/*.cxms"),
            new ChangeTagValue("/root/child", null, "newValue", null)
          ),
          text(
            """
              <root>
                  <child>value</child>
              </root>
              """,
            """
              <root>
                  <child>newValue</child>
              </root>
              """,
            spec -> spec.path("test.cxms")
          )
        );
    }
}
