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
package org.openrewrite.hcl.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class HclAttributeTest implements RewriteTest {

    @DocumentExample
    @Test
    void attributeValue() {
        rewriteRun(
          spec -> spec
            .recipe(RewriteTest.toRecipe(() -> new HclVisitor<ExecutionContext>() {
                  @Override
                  public Hcl visitBlock(Hcl.Block block, ExecutionContext executionContext) {
                      assertThat(block.<String>getAttributeValue("key"))
                        .isEqualTo("hello");
                      return block.withAttributeValue("key", "goodbye");
                  }
              }
            ).withMaxCycles(1)),
          hcl(
            """
              provider {
                  key = "hello"
              }
              """,
            """
              provider {
                  key = "goodbye"
              }
              """
          )
        );
    }

    @Test
    void booleanAttributeValue() {
        rewriteRun(
          spec -> spec.recipe(withAttributeValue("enabled", false)),
          hcl(
            """
              provider {
                  enabled = true
              }
              """,
            """
              provider {
                  enabled = false
              }
              """
          )
        );
    }

    @Test
    void numericAttributeValue() {
        rewriteRun(
          spec -> spec.recipe(withAttributeValue("count", 3)),
          hcl(
            """
              provider {
                  count = 1
              }
              """,
            """
              provider {
                  count = 3
              }
              """
          )
        );
    }

    @Test
    void unchangedNumericAttributeValue() {
        rewriteRun(
          spec -> spec.recipe(withAttributeValue("count", 1)),
          hcl(
            """
              provider {
                  count = 1
              }
              """
          )
        );
    }

    @Test
    void nonLiteralAttributeValueIsLeftAlone() {
        rewriteRun(
          spec -> spec.recipe(withAttributeValue("key", "goodbye")),
          hcl(
            """
              provider {
                  key = var.key
              }
              """
          )
        );
    }

    private static Recipe withAttributeValue(String attrName, Object value) {
        return RewriteTest.toRecipe(() -> new HclVisitor<ExecutionContext>() {
            @Override
            public Hcl visitBlock(Hcl.Block block, ExecutionContext ctx) {
                return block.withAttributeValue(attrName, value);
            }
        });
    }

    @Test
    void attribute() {
        rewriteRun(
          hcl("a = true")
        );
    }

    @Test
    void objectValueAttributes() {
        rewriteRun(
          hcl(
                """
            locals {
                simple_str = "str1"
                objectvalue = {
                  simple_attr = "value1"
                  "quoted_attr" = "value2"
                  (template_attr) = "value3"
                  "${local.simple_str}quoted_template" = "value4"
                }
            }
            """
          )
        );
    }

    @Test
    void objectKey() {
        rewriteRun(
          hcl(
            """
              terraform {
                required_providers {
                  null = {
                    source = "hashicorp/null"
                    version = "=3.2.3"
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void simpleNameOfObjectKeys() {
        List<String> names = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new HclVisitor<ExecutionContext>() {
              @Override
              public Hcl visitAttribute(Hcl.Attribute attribute, ExecutionContext ctx) {
                  names.add(attribute.getSimpleName());
                  return super.visitAttribute(attribute, ctx);
              }
          })),
          hcl(
            """
              locals {
                  objectvalue = {
                    simple_attr = "value1"
                    "quoted-attr" = "value2"
                    (template_attr) = "value3"
                    "${local.simple_str}quoted_template" = "value4"
                    (local.a) = "value5"
                    "" = "value6"
                  }
              }
              """
          )
        );
        assertThat(names).containsExactly(
          "objectvalue",
          "simple_attr",
          "quoted-attr",
          "template_attr",
          "\"${local.simple_str}quoted_template\"",
          "(local.a)",
          ""
        );
    }
}
