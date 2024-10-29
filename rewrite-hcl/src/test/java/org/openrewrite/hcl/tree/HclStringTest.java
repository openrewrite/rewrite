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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class HclStringTest implements RewriteTest {

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite/issues/4612")
    @Test
    void quoteEscaping() {
        rewriteRun(
          hcl(
            """
              locals {
                quotedText = "this is a double quote: \". Look at me"
              }
              """
          )
        );
    }

    @ExpectedToFail
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
}
