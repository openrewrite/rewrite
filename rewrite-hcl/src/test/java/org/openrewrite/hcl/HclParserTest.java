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
package org.openrewrite.hcl;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class HclParserTest implements RewriteTest {

    @Test
    void unicode() {
        rewriteRun(
          hcl(
            """
              tags = /*👇*/{
                git_file =/*👇*/ "terraform/aws/👇.tf"
                git_repo /*👇*/= "terragoat"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6446")
    @Test
    void usingIfAsAttributeName() {
        rewriteRun(
          hcl(
            """
              resource "not_a_real_one_for_sure" deny {
                if = "a"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6446")
    @Test
    void usingInAsAttributeName() {
        rewriteRun(
          hcl(
            """
              resource "not_a_real_one_for_sure" deny {
                in = "a"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6446")
    @Test
    void keywordsAsTopLevelAttributes() {
        rewriteRun(
          hcl(
            """
              if = "a"
              in = "b"
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6446")
    @Test
    void keywordAttributeAlongsideForExpression() {
        rewriteRun(
          hcl(
            """
              filtered = [for x in items : x if x > 0]
              if       = "still works"
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6446")
    @Test
    void keywordsAsObjectKeys() {
        rewriteRun(
          hcl(
            """
              tags = {
                if = "a"
                in = "b"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6446")
    @Test
    void keywordObjectKeyMixedWithRegularKeys() {
        rewriteRun(
          hcl(
            """
              tags = {
                name = "x"
                if   = "y"
                env  = "z"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6446")
    @Test
    void keywordAttributeWithCommentAndUnusualSpacing() {
        rewriteRun(
          hcl(
            """
              # condition placeholder
              block "x" {
                if    = "a"
                /* trailing */ in = "b"
              }
              """
          )
        );
    }

    @Test
    void escapes() {
        rewriteRun(
          hcl(
            """
              variable "password_mask" {
                description = "Characters not allowed in generated passwords."
                type        = string
                default     = "'()*+,./:;=?[]`{|}~\\"\\\\"  # fails only because escaped backslash is last character
              }
              """
          )
        );
    }
}
