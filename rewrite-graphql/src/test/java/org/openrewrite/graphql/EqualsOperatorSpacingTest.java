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

class EqualsOperatorSpacingTest implements RewriteTest {

    @Test
    void variableDefaultValueSingleSpace() {
        rewriteRun(
            graphQl(
                """
                query GetUser($id: ID = "123") {
                  user(id: $id) {
                    name
                  }
                }
                """
            )
        );
    }

    @Test
    void variableDefaultValueMultipleSpaces() {
        rewriteRun(
            graphQl(
                """
                query GetUser($id: ID   =   "123") {
                  user(id: $id) {
                    name
                  }
                }
                """
            )
        );
    }

    @Test
    void variableDefaultValueNoSpace() {
        rewriteRun(
            graphQl(
                """
                query GetUser($id: ID="123") {
                  user(id: $id) {
                    name
                  }
                }
                """
            )
        );
    }

    @Test
    void inputFieldDefaultValueSingleSpace() {
        rewriteRun(
            graphQl(
                """
                input UserInput {
                  name: String = "Anonymous"
                  age: Int = 0
                }
                """
            )
        );
    }

    @Test
    void inputFieldDefaultValueMultipleSpaces() {
        rewriteRun(
            graphQl(
                """
                input UserInput {
                  name: String    =    "Anonymous"
                  age: Int\t=\t0
                }
                """
            )
        );
    }

    @Test
    void argumentDefaultValueSingleSpace() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  users(limit: Int = 10): [User!]!
                }
                """
            )
        );
    }

    @Test
    void argumentDefaultValueNoSpace() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  users(limit: Int=10, offset: Int=0): [User!]!
                }
                """
            )
        );
    }

    @Test
    void unionTypeSingleSpace() {
        rewriteRun(
            graphQl(
                """
                union SearchResult = User | Post | Comment
                """
            )
        );
    }

    @Test
    void unionTypeMultipleSpaces() {
        rewriteRun(
            graphQl(
                """
                union SearchResult    =    User | Post | Comment
                """
            )
        );
    }

    @Test
    void unionTypeNoSpace() {
        rewriteRun(
            graphQl(
                """
                union SearchResult=User | Post | Comment
                """
            )
        );
    }

    @Test
    void unionTypeNewlineBeforeEquals() {
        rewriteRun(
            graphQl(
                """
                union SearchResult
                  = User | Post | Comment
                """
            )
        );
    }

    @Test
    void extendUnionSpacing() {
        rewriteRun(
            graphQl(
                """
                extend union SearchResult   =   Video | Image
                """
            )
        );
    }

    @Test
    void directiveArgumentDefaultValue() {
        rewriteRun(
            graphQl(
                """
                directive @example(arg: String = "default") on FIELD
                """
            )
        );
    }

    @Test
    void multipleDefaultValues() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  search(
                    query: String = ""
                    limit: Int=10
                    offset: Int  =  0
                  ): [Result!]!
                }
                """
            )
        );
    }
}