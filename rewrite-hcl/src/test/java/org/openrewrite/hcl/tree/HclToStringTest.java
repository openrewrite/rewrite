/*
 * Copyright 2026 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class HclToStringTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "42",
      "null",
      "var",
      "var.name",
      "var.items.0",
      "1 + 2",
      "true ? 1 : 2",
      "[for v in var.items : v]",
      "{for k, v in var.items : k => v}",
      "join(\",\", [\"a\", \"b\"])",
      "<<EOF\nhello ${var.name}\nEOF",
      "var.items[0]",
      "{ key = 1, other = 2 }",
      "(1 + 2)",
      "\"hello ${var.name}\"",
      "var.items[*]",
      "var.items.*",
      "[1, 2]",
      "-1",
      "!true",
      "1 /* keep */ + 2"
    })
    void expressions(String expression) {
        rewriteRun(
          hcl(
            "# leading comment\nvalue = /* leading value comment */ " + expression + "\n",
            spec -> spec.afterRecipe(config -> {
                Hcl.Attribute attribute = (Hcl.Attribute) config.getBody().getFirst();
                assertThat(attribute.getValue().toString()).isEqualTo(expression);
                assertThat(attribute.getName().toString()).isEqualTo("value");
            })
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"  hello  ", "   ", "\thello\t"})
    void quotedTemplateLiteralWhitespace(String text) {
        rewriteRun(
          hcl(
            "value = \"" + text + "\"",
            spec -> spec.afterRecipe(config -> {
                Hcl.QuotedTemplate template = (Hcl.QuotedTemplate) ((Hcl.Attribute) config.getBody().getFirst()).getValue();
                assertThat(template.getExpressions().getFirst().toString()).isEqualTo(text);
            })
          )
        );
    }

    @Test
    void heredocLiteralWhitespace() {
        rewriteRun(
          hcl(
            "value = <<EOF\n  hello\nEOF\n",
            spec -> spec.afterRecipe(config -> {
                Hcl.HeredocTemplate template = (Hcl.HeredocTemplate) ((Hcl.Attribute) config.getBody().getFirst()).getValue();
                assertThat(template.getExpressions().getFirst().toString()).isEqualTo("  hello");
            })
          )
        );
    }

    @Test
    void configFileBlockAndAttribute() {
        rewriteRun(
          hcl(
            """
              # leading comment
              resource "example" "name" {
                # keep this comment
                value = 42
              }
              """,
            spec -> spec.afterRecipe(config -> {
                String expected = """
                  resource "example" "name" {
                    # keep this comment
                    value = 42
                  }""";
                assertThat(config.toString()).isEqualTo(expected);
                Hcl.Block block = (Hcl.Block) config.getBody().getFirst();
                assertThat(block.toString()).isEqualTo(expected);
                assertThat(block.getBody().getFirst().toString()).isEqualTo("value = 42");
            })
          )
        );
    }

    @Test
    void nestedNodes() {
        rewriteRun(
          hcl(
            """
              tuple = [for v in var.items : v]
              index = var.items[ 0 ]
              full = var.items[ * ]
              attr = var.items.*
              template = "hello ${var.name}"
              """,
            spec -> spec.afterRecipe(config -> {
                Hcl.ForTuple tuple = (Hcl.ForTuple) ((Hcl.Attribute) config.getBody().get(0)).getValue();
                assertThat(tuple.getIntro().toString()).isEqualTo("for v in var.items");
                Hcl.Index index = (Hcl.Index) ((Hcl.Attribute) config.getBody().get(1)).getValue();
                assertThat(index.getPosition().toString()).isEqualTo("[ 0 ]");
                Hcl.Splat full = (Hcl.Splat) ((Hcl.Attribute) config.getBody().get(2)).getValue();
                assertThat(full.getOperator().toString()).isEqualTo("[ * ]");
                assertThat(full.getOperator().getSplat().getElement().toString()).isEmpty();
                Hcl.Splat attr = (Hcl.Splat) ((Hcl.Attribute) config.getBody().get(3)).getValue();
                assertThat(attr.getOperator().toString()).isEqualTo(".*");
                Hcl.QuotedTemplate template = (Hcl.QuotedTemplate) ((Hcl.Attribute) config.getBody().get(4)).getValue();
                assertThat(template.getExpressions().get(1).toString()).isEqualTo("${var.name}");
            })
          )
        );
    }
}
