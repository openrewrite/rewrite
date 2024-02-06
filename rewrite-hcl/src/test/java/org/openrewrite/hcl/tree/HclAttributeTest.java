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
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class HclAttributeTest implements RewriteTest {

    @Test
    void attribute() {
        rewriteRun(
          hcl("a = true")
        );
    }

    @Test
    void objectValueAttributes() {
        rewriteRun(
          hcl("""
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
}
