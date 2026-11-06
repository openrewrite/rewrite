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
package org.openrewrite.hcl.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class HclStringTest implements RewriteTest {

    @Test
    void literalValueHoldsTheTextTheEscapesStandFor() {
        rewriteRun(
          hcl(
            """
              resource "a\\"b" x {
                quoted = "\\u00e9 \\U0001F600 \\"q\\" \\\\ \\n $${x} %%{y} $$z"
                heredoc = <<EOT
              \\n $${x}
              EOT
              }
              """,
            spec -> spec.afterRecipe(config -> {
                List<Object> values = new ArrayList<>();
                new HclIsoVisitor<List<Object>>() {
                    @Override
                    public Hcl.Literal visitLiteral(Hcl.Literal literal, List<Object> found) {
                        found.add(literal.getValue());
                        return literal;
                    }
                }.visit(config, values);
                assertThat(values).containsExactly(
                  "a\"b",
                  "é 😀 \"q\" \\ \n ${x} %{y} $$z",
                  "\\n ${x}"
                );
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4612")
    @Test
    void quoteEscaping() {
        rewriteRun(
          hcl(
            """
              locals {
                quotedText = "this is a double quote: \\". Look at me"
              }
              """
          )
        );
    }

    @Test
    void escapingTheDollarSign() {
        // https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#template-literals
        // The interpolation and directive introductions are escaped by doubling their leading characters. The ${
        // sequence is escaped as $${ and the %{ sequence is escaped as %%{.
        rewriteRun(
          hcl(
            """
              locals {
                something = "Not a template/expression, just escaping $${somethingElse}."
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4613")
    @Test
    void trailingDollarSign() {
        rewriteRun(
          hcl(
            """
              locals {
                regexp = "^(.*?)-monitoring$"
              }
              """
          )
        );
    }

    @Test
    void slashesInStrings() {
        rewriteRun(
          hcl(
            """
              locals {
                cidr = "192.168.0.0/24"
              }
              """
          )
        );
    }
}
