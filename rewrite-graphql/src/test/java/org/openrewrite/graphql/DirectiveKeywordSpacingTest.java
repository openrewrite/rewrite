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
package org.openrewrite.graphql;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.graphql.Assertions.graphQl;

class DirectiveKeywordSpacingTest implements RewriteTest {

    @Test
    void singleSpaceBeforeRepeatable() {
        rewriteRun(
            graphQl(
                """
                directive @example(arg: String) repeatable on FIELD
                """
            )
        );
    }

    @Test
    void multipleSpacesBeforeRepeatable() {
        rewriteRun(
            graphQl(
                """
                directive @example(arg: String)    repeatable on FIELD
                """
            )
        );
    }

    @Test
    void tabBeforeRepeatable() {
        rewriteRun(
            graphQl(
                """
                directive @example(arg: String)\trepeatable on FIELD
                """
            )
        );
    }

    @Test
    void newlineBeforeRepeatable() {
        rewriteRun(
            graphQl(
                """
                directive @example(arg: String)
                  repeatable on FIELD
                """
            )
        );
    }

    @Test
    void singleSpaceBeforeOn() {
        rewriteRun(
            graphQl(
                """
                directive @example on FIELD
                """
            )
        );
    }

    @Test
    void multipleSpacesBeforeOn() {
        rewriteRun(
            graphQl(
                """
                directive @example    on    FIELD
                """
            )
        );
    }

    @Test
    void tabBeforeOn() {
        rewriteRun(
            graphQl(
                """
                directive @example\ton FIELD
                """
            )
        );
    }

    @Test
    void newlineBeforeOn() {
        rewriteRun(
            graphQl(
                """
                directive @example
                  on FIELD
                """
            )
        );
    }

    @Test
    void repeatableWithVaryingSpacing() {
        rewriteRun(
            graphQl(
                """
                directive @example   repeatable   on   FIELD | ARGUMENT_DEFINITION
                """
            )
        );
    }

    @Test
    void noRepeatable() {
        rewriteRun(
            graphQl(
                """
                directive @simple on QUERY | MUTATION
                """
            )
        );
    }
}