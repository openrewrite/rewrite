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
package org.openrewrite.hcl.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class AutoFormatTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormat());
    }

    @Test
    void forLoops() {
        rewriteRun(
          hcl(
          """
          a = [for v in ["a", "b"] : v]
          b = [for i, v in ["a", "b"] : i]
          c = [for i, v in ["a", "b", "c"]: v if 1 && !0]
          """
          )
        );
    }

    @Test
    void objectValues() {
        rewriteRun(
          hcl(
        """
              locals {
                object = {
                       string_attr = "value1"
                       int_attr    = 2
                }
              }
              """,
            """
              locals {
                object = {
                  string_attr = "value1"
                  int_attr    = 2
                }
              }
              """
          )
        );
    }

    @Test
    void objectValuesCommas() {
        rewriteRun(
          hcl(
            """
              locals {
                object = {
                       string_attr = "value1"   ,   
                       int_attr = 2,
                }
              }
              """,
            """
              locals {
                object = {
                  string_attr = "value1",
                  int_attr    = 2,
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/974")
    @Test
    void lineComments() {
        rewriteRun(
          hcl(
            """
              # a hash comment with # or // is still 1 line.
              // a slash comment with # or // is still 1 line.
              resource {
              }
              """
          )
        );
    }

    @Test
    void blankLines() {
        rewriteRun(
          hcl(
            """
              r1 {
              }
              
              
              
              r2 {
              }
              """,
            """
              r1 {
              }
              
              r2 {
              }
              """
          )
        );
    }
}
