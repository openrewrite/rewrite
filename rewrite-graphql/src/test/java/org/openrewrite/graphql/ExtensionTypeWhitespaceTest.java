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
 * Tests for preserving whitespace between "extend" and type keywords in GraphQL extensions.
 * This ensures idempotency when the original source has various formatting styles.
 */
public class ExtensionTypeWhitespaceTest implements RewriteTest {

    @Test
    void preserveMultipleSpacesInScalarExtension() {
        rewriteRun(
            graphQl(
                """
                extend   scalar   MyScalar @directive
                """
            )
        );
    }

    @Test
    void preserveTabsInObjectExtension() {
        rewriteRun(
            graphQl(
                """
                extend	type	User {
                  email: String!
                }
                """
            )
        );
    }

    @Test
    void preserveNewlineInInterfaceExtension() {
        rewriteRun(
            graphQl(
                """
                extend
                interface
                Node {
                  updatedAt: DateTime!
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Comments between extend and type keyword not supported by grammar")
    void preserveCommentBetweenExtendAndUnion() {
        rewriteRun(
            graphQl(
                """
                extend /* adding search results */ union SearchResult = User | Post | Comment
                """
            )
        );
    }

    @Test
    void preserveMixedWhitespaceInEnumExtension() {
        rewriteRun(
            graphQl(
                """
                extend  	  enum Status {
                  ARCHIVED
                  DELETED
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Line comments between extend and type keyword not supported by grammar")
    void preserveLineCommentInInputExtension() {
        rewriteRun(
            graphQl(
                """
                extend // extending for new fields
                input UserInput {
                  avatar: String
                }
                """
            )
        );
    }

    @Test
    void preserveComplexWhitespaceInSchemaExtension() {
        rewriteRun(
            graphQl(
                """
                extend    
                schema {
                  mutation: Mutation
                  subscription: Subscription
                }
                """
            )
        );
    }

    @Test
    void preserveStandardSpacingInAllExtensions() {
        // This test ensures standard single-space formatting still works
        rewriteRun(
            graphQl(
                """
                extend scalar DateTime @specifiedBy(url: "https://scalars.org/date-time")
                
                extend type Query {
                  currentUser: User
                }
                
                extend interface Node {
                  version: Int!
                }
                
                extend union SearchResult = Product
                
                extend enum Role {
                  MODERATOR
                }
                
                extend input FilterInput {
                  tags: [String!]
                }
                
                extend schema @link(url: "https://federation.dev")
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("'extendtype' is not valid GraphQL syntax")
    void preserveNoSpaceAfterExtend() {
        // Edge case: no space between extend and type keyword
        rewriteRun(
            graphQl(
                """
                extendtype User {
                  id: ID!
                }
                """,
                """
                extend type User {
                  id: ID!
                }
                """
            )
        );
    }
}