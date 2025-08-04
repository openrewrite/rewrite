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

class HclEmptyTest implements RewriteTest {

    @Test
    void preserveNewlineInEmptyObject() {
        rewriteRun(
          hcl(
            """
              locals {
                myvar = {
                }
              }
              """
          )
        );
    }

    @Test
    void preserveMultipleNewlinesInEmptyObject() {
        rewriteRun(
          hcl(
            """
              resource "example" "test" {
                config = {
              
              
                }
              }
              """
          )
        );
    }

    @Test
    void preserveMixedWhitespaceInEmptyObject() {
        rewriteRun(
          hcl(
            """
              module "example" {
                settings = {
                \
                  # TODO: Add settings here
                \
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5844")
    @Test
    void preserveInModule() {
        rewriteRun(
          hcl(
            """
              module "foo" {
                  bar = {
                  }
              }
              """
          )
        );
    }
}
