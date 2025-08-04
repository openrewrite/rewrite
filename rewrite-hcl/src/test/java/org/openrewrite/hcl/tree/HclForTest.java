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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class HclForTest implements RewriteTest {

    @Test
    void forTuple() {
        rewriteRun(
          hcl(
            """
              a = [ for v in ["a", "b"] : v ]
              b = [ for i, v in ["a", "b"] : i ]
              c = [for i, v in ["a", "b", "c"]: v if 1 && !0]
              d = [
                for i, v in ["a", "b", "c"]: v if 0 || 1
              ]
              e = [
                for i, v in ["a", "b", "c"]: v
                if 0
              ]
              """
          )
        );
    }

    @Test
    void forObject() {
        rewriteRun(
          hcl(
            """
              a = { for i, v in ["a", "b"]: v => i }
              b = { for i, v in ["a", "a", "b"]: k => v }
              c = {
                for i, v in ["a", "a", "b"]: v => i...
              }
              d = {
                for i, v in ["a", "a", "b"]: v => i...
                if 0
              }
              """
          )
        );
    }

    @Test
    void forEach() {
        rewriteRun(
          hcl(
            """
              resource "aws_iam_user" "the-accounts" {
                for_each = toset( ["Todd", "James", "Alice", "Dottie"] )
                name     = each.key
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4857")
    @Test
    void commentInAFor() {
        rewriteRun(
          hcl(
            """
              locals {
               a = {
                 # this is some super smart logic here
                 for i, v in ["a", "b"]: v => i
               }
              }
              """
          )
        );
    }
}
