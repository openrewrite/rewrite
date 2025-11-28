/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.format.RemoveTrailingWhitespace;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.javascript.Assertions.javascript;

class RemoveTrailingWhitespaceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveTrailingWhitespace());
    }

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6274")
    @Test
    void doNotRemoveShebang() {
        rewriteRun(
          javascript(
            """
              #!/usr/bin/env node

              /**
              * Generate llms.txt and llms-full.txt for OpenRewrite documentation
              */
              """
          )
        );
    }
}
