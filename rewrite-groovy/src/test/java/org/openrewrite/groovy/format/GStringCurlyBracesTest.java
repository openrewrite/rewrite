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
package org.openrewrite.groovy.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class GStringCurlyBracesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new GStringCurlyBraces());
    }

    @Test
    void basic() {
        rewriteRun(
          groovy("""
            def name = 'world'
            "Hello $name!"
            """,
            """
            def name = 'world'
            "Hello ${name}!"
            """)
        );
    }

    @Test
    void fieldAccess() {
        rewriteRun(
          groovy("""
            def to = [ you : 'world']
            "Hello $to.you!"
            """,
            """
            def to = [ you : 'world']
            "Hello ${to.you}!"
            """)
        );
    }
}
