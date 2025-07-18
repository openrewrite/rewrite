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
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.graphql.Assertions.graphQl;

class ObjectValueWhitespaceTest implements RewriteTest {

    @Test
    void simpleObjectValue() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(filter: { name: "test" })
                }
                """
            )
        );
    }

    @Test
    void objectValueWithSpaceBeforeClosingBrace() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(filter: { name: "test", active: true })
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires AST changes to properly preserve trailing commas - same limitation as ArgumentFormattingTest")
    void nestedObjectValues() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(filter: {
                    user: { name: "test" },
                    settings: { active: true }
                  })
                }
                """
            )
        );
    }

    @Test
    void objectValueInArgument() {
        rewriteRun(
            graphQl(
                """
                query {
                  posts(
                    orderBy: { field: CREATED_AT, direction: DESC }
                  ) {
                    id
                  }
                }
                """
            )
        );
    }

}