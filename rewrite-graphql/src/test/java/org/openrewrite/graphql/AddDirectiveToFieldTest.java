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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.graphql.Assertions.graphQl;

class AddDirectiveToFieldTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddDirectiveToField("User", "email", "@auth"));
    }

    @Test
    void addDirectiveToFieldWithoutDirectives() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String!
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @auth
                }
                """
            )
        );
    }

    @Test
    void addDirectiveToFieldWithExistingDirective() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @deprecated
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @deprecated @auth
                }
                """
            )
        );
    }

    @Test
    void addDirectiveToFieldWithMultipleExistingDirectives() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @deprecated @private
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @deprecated @private @auth
                }
                """
            )
        );
    }

    @Test
    void addDirectiveWithArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: ADMIN)")),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String!
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @auth(role: ADMIN)
                }
                """
            )
        );
    }

    @Test
    void addDirectiveWithComplexArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(roles: [ADMIN, MODERATOR], scope: \"user:email\")")),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String!
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @auth(roles: [ADMIN, MODERATOR], scope: "user:email")
                }
                """
            )
        );
    }

    @Test
    void doNotAddDuplicateDirective() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @auth
                }
                """
            )
        );
    }

    @Test
    void doNotAddDuplicateDirectiveWithArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: ADMIN)")),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @auth(role: ADMIN)
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Extra space after block string")
    @Test
    void preserveFieldDescription() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  "The user's email address"
                  email: String!
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  "The user's email address"
                  email: String! @auth
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Missing comma between arguments")
    @Test
    void preserveFieldArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("Query", "users", "@auth")),
            graphQl(
                """
                type Query {
                  users(limit: Int, offset: Int): [User!]!
                }
                """,
                """
                type Query {
                  users(limit: Int, offset: Int): [User!]! @auth
                }
                """
            )
        );
    }

    @Test
    void doNotModifyOtherTypes() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  email: String!
                }
                
                type Post {
                  id: ID!
                  email: String!
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth
                }
                
                type Post {
                  id: ID!
                  email: String!
                }
                """
            )
        );
    }

    @Test
    void doNotModifyOtherFields() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  email: String!
                  emailVerified: Boolean!
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth
                  emailVerified: Boolean!
                }
                """
            )
        );
    }

    @Test
    void handleInterfaceFields() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("Node", "id", "@key")),
            graphQl(
                """
                interface Node {
                  id: ID!
                  createdAt: DateTime!
                }
                """,
                """
                interface Node {
                  id: ID! @key
                  createdAt: DateTime!
                }
                """
            )
        );
    }

    @Test
    void handleFieldsInMultipleTypes() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField(null, "id", "@key")),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                }
                
                type Post {
                  id: ID!
                  title: String!
                }
                """,
                """
                type User {
                  id: ID! @key
                  name: String!
                }
                
                type Post {
                  id: ID! @key
                  title: String!
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Inline comment preservation")
    @Test
    void preserveWhitespaceAndComments() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  # User's email address
                  email: String!
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  # User's email address
                  email: String! @auth
                }
                """
            )
        );
    }

    @Test
    void handleFieldWithComplexType() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("Query", "users", "@auth")),
            graphQl(
                """
                type Query {
                  users: [User!]!
                  user(id: ID!): User
                }
                """,
                """
                type Query {
                  users: [User!]! @auth
                  user(id: ID!): User
                }
                """
            )
        );
    }

    @Test
    void handleExtensionTypes() {
        rewriteRun(
            graphQl(
                """
                extend type User {
                  email: String!
                  phone: String
                }
                """,
                """
                extend type User {
                  email: String! @auth
                  phone: String
                }
                """
            )
        );
    }

    @Test
    void doNotAddIfDirectiveAlreadyExistsWithDifferentArguments() {
        // This test ensures we don't add a duplicate @auth directive even with different arguments
        // The user would need to use a different recipe to modify existing directive arguments
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: USER)")),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @auth(role: ADMIN)
                }
                """
            )
        );
    }

    @Test
    void overrideExistingDirectiveWithoutArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: ADMIN)", true)).expectedCyclesThatMakeChanges(2),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @auth
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth(role: ADMIN)
                }
                """
            )
        );
    }

    @Test
    void overrideExistingDirectiveWithArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: USER)", true)).expectedCyclesThatMakeChanges(2),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @auth(role: ADMIN)
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth(role: USER)
                }
                """
            )
        );
    }

    @Test
    void overrideExistingDirectiveAndPreserveOthers() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: ADMIN)", true)).expectedCyclesThatMakeChanges(2),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @deprecated @auth @private
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @deprecated @auth(role: ADMIN) @private
                }
                """
            )
        );
    }

    @Test
    void overrideDirectiveWithComplexArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(roles: [USER], scope: \"read\")", true)).expectedCyclesThatMakeChanges(2),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @auth(roles: [ADMIN, MODERATOR], scope: "write")
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth(roles: [USER], scope: "read")
                }
                """
            )
        );
    }

    @Test
    void overrideToRemoveArguments() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth", true)).expectedCyclesThatMakeChanges(2),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @auth(role: ADMIN)
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth
                }
                """
            )
        );
    }

    @Test
    void doNotOverrideWhenOverrideIsFalse() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: USER)", false)),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @auth(role: ADMIN)
                }
                """
            )
        );
    }

    @Test
    void overrideDirectiveInMiddleOfList() {
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@deprecated(reason: \"Use newEmail instead\")", true)).expectedCyclesThatMakeChanges(2),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String! @auth @deprecated @private
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth @deprecated(reason: "Use newEmail instead") @private
                }
                """
            )
        );
    }

    @Test
    void addDirectiveWithOverrideEnabledButNoExisting() {
        // When override is true but directive doesn't exist, it should still add it
        rewriteRun(
            spec -> spec.recipe(new AddDirectiveToField("User", "email", "@auth(role: ADMIN)", true)).expectedCyclesThatMakeChanges(2),
            graphQl(
                """
                type User {
                  id: ID!
                  email: String!
                }
                """,
                """
                type User {
                  id: ID!
                  email: String! @auth(role: ADMIN)
                }
                """
            )
        );
    }
}