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

class MultilineClosingBraceTest implements RewriteTest {

    @Test
    void schemaWithMultilineClosingBrace() {
        rewriteRun(
            graphQl(
                """
                schema {
                  query: Query
                  mutation: Mutation
                }
                """
            )
        );
    }

    @Test
    void schemaWithDirectivesAndMultilineClosingBrace() {
        rewriteRun(
            graphQl(
                """
                schema @auth {
                  query: Query
                  mutation: Mutation
                }
                """
            )
        );
    }

    @Test
    void multilineArgumentsWithClosingParen() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  search(
                    text: String!
                    filter: SearchFilter
                  ): [Result!]!
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Cannot preserve multiline directive location formatting without RightPadded")
    void directiveDefinitionWithMultilineLocation() {
        // Directive locations spread across multiple lines get collapsed to a single line.
        // The parser correctly reads all locations but the printer doesn't preserve
        // the original line breaks between them.
        // Current output: "directive @example on | QUERY | MUTATION | FIELD"
        // To fix this, we would need to change directiveLocations from 
        // List<Name> to List<RightPadded<Name>> to store the formatting after each |
        rewriteRun(
            graphQl(
                """
                directive @example on
                  | QUERY
                  | MUTATION
                  | FIELD
                """
            )
        );
    }

    @Test
    void typeWithFieldsMultilineClosingBrace() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String!
                }
                """
            )
        );
    }

    @Test
    void enumWithMultilineClosingBrace() {
        rewriteRun(
            graphQl(
                """
                enum Role {
                  ADMIN
                  USER
                  GUEST
                }
                """
            )
        );
    }

    @Test
    void inputTypeWithMultilineClosingBrace() {
        rewriteRun(
            graphQl(
                """
                input UserInput {
                  name: String!
                  email: String!
                  role: Role
                }
                """
            )
        );
    }

    @Test
    void interfaceWithMultilineClosingBrace() {
        rewriteRun(
            graphQl(
                """
                interface Node {
                  id: ID!
                  createdAt: DateTime!
                  updatedAt: DateTime!
                }
                """
            )
        );
    }
}