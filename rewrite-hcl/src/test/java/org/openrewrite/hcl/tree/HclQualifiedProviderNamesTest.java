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
package org.openrewrite.hcl.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class HclQualifiedProviderNamesTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2421")
    @Test
    void qualifiedProviderNames() {
        rewriteRun(
          hcl(
            """
              module "tunnel" {
                source = "./tunnel"
                providers = {
                  aws.src = aws.usw1
                  aws.dst = aws.usw2
                }
              }
              """
          )
        );
    }

    @Test
    void qualifiedProviderNamesWithMultipleLevels() {
        rewriteRun(
          hcl(
            """
              module "example" {
                source = "./example"
                providers = {
                  aws.region.az = aws.us_east_1a
                  gcp.project.zone = gcp.my_project.us_central1_a
                }
              }
              """
          )
        );
    }

    @Test
    void mixedProviderNames() {
        rewriteRun(
          hcl(
            """
              module "mixed" {
                source = "./mixed"
                providers = {
                  simple = provider.simple
                  aws.qualified = aws.region
                  gcp.multi.level = gcp.project.zone
                }
              }
              """
          )
        );
    }

    @Test
    void qualifiedProviderNamesWithColonSeparator() {
        rewriteRun(
          hcl(
            """
              module "colon_style" {
                source = "./colon"
                providers = {
                  aws.src : aws.usw1
                  aws.dst : aws.usw2
                }
              }
              """
          )
        );
    }
}
