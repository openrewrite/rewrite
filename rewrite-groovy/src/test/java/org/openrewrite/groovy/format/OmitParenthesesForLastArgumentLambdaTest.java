/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.groovy.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.groovy.Assertions.groovy;

class OmitParenthesesForLastArgumentLambdaTest implements RewriteTest {
    final SourceSpecs closureApi = groovy(
      """
        class Test {
          static void test(Closure closure) {
          }
        }
        """
    );

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OmitParenthesesForLastArgumentLambda());
    }

    @Disabled("Not yet implemented")
    @Test
    void lastClosureArgument() {
        rewriteRun(
          closureApi,
          groovy(
            "Test.test({ it })",
            "Test.test { it }"
          )
        );
    }
}
