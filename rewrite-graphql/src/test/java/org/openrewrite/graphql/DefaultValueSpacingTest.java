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

/**
 * Tests for preserving whitespace around default value equals signs.
 * This demonstrates the current limitation where custom spacing is normalized.
 */
public class DefaultValueSpacingTest implements RewriteTest {

    @Test
    @org.junit.jupiter.api.Disabled("Current limitation: space before = is always normalized")
    void preserveNoSpaceBeforeEquals() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  search(query: String="default"): [Result]
                }
                """,
                """
                type Query {
                  search(query: String = "default"): [Result]
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Current limitation: space before = is always normalized")
    void preserveMultipleSpacesBeforeEquals() {
        rewriteRun(
            graphQl(
                """
                input Filter {
                  status: Status    =    ACTIVE
                  limit: Int  =  10
                }
                """,
                """
                input Filter {
                  status: Status = ACTIVE
                  limit: Int = 10
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Current limitation: space before = is always normalized")
    void preserveNewlineBeforeEquals() {
        rewriteRun(
            graphQl(
                """
                query GetItems($filter: Filter
                  = { status: ACTIVE }) {
                  items(filter: $filter) {
                    id
                  }
                }
                """,
                """
                query GetItems($filter: Filter = { status: ACTIVE }) {
                  items(filter: $filter) {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Current limitation: comments before = are lost")
    void preserveCommentBeforeEquals() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  search(
                    query: String # search term
                    = "default"
                  ): [Result]
                }
                """,
                """
                type Query {
                  search(
                    query: String = "default"
                  ): [Result]
                }
                """
            )
        );
    }
}