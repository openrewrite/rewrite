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
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.hcl.Assertions.hcl;

class HclFunctionCallTest implements RewriteTest {

    @Test
    void functionCall() {
        rewriteRun(
          hcl(
            """
              a = method (1, 2 )
              b = method ( )
              """
          )
        );
    }

    @Test
    void providerScopedFunctionCall() {
        rewriteRun(
          hcl(
            """
              terraform {
                  required_providers {
                      test = {
                          source = "hashicorp/test"
                      }
                  }
              }
              locals {
                  result = provider::test::count_e("cheese")
              }
              """,
             spec -> spec.afterRecipe(configFile -> {
                 Hcl.Block locals = (Hcl.Block) configFile.getBody().getLast();
                 Hcl.Attribute result = (Hcl.Attribute) locals.getBody().getFirst();
                 Hcl.FunctionCall countE = (Hcl.FunctionCall) result.getValue();
                 assertThat(countE.getName().getName()).isEqualTo("provider::test::count_e");
             })
          )
        );
    }

    @Test
    void multipleProviderScopedFunctions() {
        rewriteRun(
          hcl(
            """
              locals {
                  a = provider::aws::ec2_instance_type_info("t2.micro")
                  b = provider::google::compute_zone("us-central1-a")
                  c = normalFunction("arg")
              }
              """
          )
        );
    }
}
