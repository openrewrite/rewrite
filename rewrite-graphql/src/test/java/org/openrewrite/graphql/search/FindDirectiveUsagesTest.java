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
package org.openrewrite.graphql.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import org.openrewrite.graphql.search.FindDirectiveUsages;

import static org.openrewrite.graphql.Assertions.graphQl;

public class FindDirectiveUsagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindDirectiveUsages("deprecated"));
    }

    @Test
    @DocumentExample
    void findDirectiveOnFields() {
        rewriteRun(
            graphQl(
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! @deprecated(reason: "Use emailAddress")
                  emailAddress: String!
                  oldField: String @deprecated
                }
                
                type Query {
                  user(id: ID!): User
                  users: [User!]! @deprecated(reason: "Use userList")
                }
                """,
                """
                type User {
                  id: ID!
                  name: String!
                  email: String! ~~>@deprecated(reason: "Use emailAddress")
                  emailAddress: String!
                  oldField: String ~~>@deprecated
                }
                
                type Query {
                  user(id: ID!): User
                  users: [User!]! ~~>@deprecated(reason: "Use userList")
                }
                """
            )
        );
    }

    @Test
    void findDirectiveDefinition() {
        rewriteRun(
            graphQl(
                """
                directive @deprecated(
                  reason: String = "No longer supported"
                ) on FIELD_DEFINITION |ENUM_VALUE
                
                type User {
                  name: String! @deprecated
                }
                """,
                """
                ~~>directive @deprecated(
                  reason: String = "No longer supported"
                ) on FIELD_DEFINITION |ENUM_VALUE
                
                type User {
                  name: String! ~~>@deprecated
                }
                """
            )
        );
    }

    @Test
    void findDirectiveOnArguments() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  search(
                    text: String! @deprecated(reason: "Use filter instead")
                    filter: SearchFilter
                  ): [Result!]!
                }
                
                input SearchFilter {
                  text: String
                  includeArchived: Boolean @deprecated
                }
                """,
                """
                type Query {
                  search(
                    text: String! ~~>@deprecated(reason: "Use filter instead")
                    filter: SearchFilter
                  ): [Result!]!
                }
                
                input SearchFilter {
                  text: String
                  includeArchived: Boolean ~~>@deprecated
                }
                """
            )
        );
    }

    @Test
    void findDirectiveOnEnumValues() {
        rewriteRun(
            graphQl(
                """
                enum UserRole {
                  ADMIN
                  USER
                  GUEST @deprecated(reason: "Use VIEWER")
                  VIEWER
                  MODERATOR @deprecated
                }
                
                type User {
                  role: UserRole!
                }
                """,
                """
                enum UserRole {
                  ADMIN
                  USER
                  GUEST ~~>@deprecated(reason: "Use VIEWER")
                  VIEWER
                  MODERATOR ~~>@deprecated
                }
                
                type User {
                  role: UserRole!
                }
                """
            )
        );
    }

    @Test
    void findDirectiveOnTypes() {
        rewriteRun(
            spec -> spec.recipe(new FindDirectiveUsages("external")),
            graphQl(
                """
                type User @external {
                  id: ID!
                  name: String!
                }
                
                type Product @external @key(fields: "id") {
                  id: ID!
                  title: String!
                }
                
                type Order {
                  id: ID!
                  user: User
                }
                """,
                """
                type User ~~>@external {
                  id: ID!
                  name: String!
                }
                
                type Product ~~>@external @key(fields: "id") {
                  id: ID!
                  title: String!
                }
                
                type Order {
                  id: ID!
                  user: User
                }
                """
            )
        );
    }

    @Test
    void findDirectiveOnSchemaAndOperations() {
        rewriteRun(
            spec -> spec.recipe(new FindDirectiveUsages("auth")),
            graphQl(
                """
                schema @auth {
                  query: Query
                  mutation: Mutation
                }
                
                type Query {
                  me: User @auth(requires: "USER")
                  admin: AdminPanel @auth(requires: "ADMIN")
                }
                
                type Mutation @auth(requires: "USER") {
                  updateProfile(data: ProfileInput!): User
                }
                """,
                """
                schema ~~>@auth {
                  query: Query
                  mutation: Mutation
                }
                
                type Query {
                  me: User ~~>@auth(requires: "USER")
                  admin: AdminPanel ~~>@auth(requires: "ADMIN")
                }
                
                type Mutation ~~>@auth(requires: "USER") {
                  updateProfile(data: ProfileInput!): User
                }
                """
            )
        );
    }

    @Test
    void findDirectiveInQueries() {
        rewriteRun(
            spec -> spec.recipe(new FindDirectiveUsages("include")),
            graphQl(
                """
                query  GetUser($withEmail: Boolean!) {
                  user {
                    id
                    name
                    email @include(if: $withEmail)
                    profile @include(if: true) {
                      bio
                      avatar
                    }
                  }
                }
                
                fragment UserFields on User {
                  posts @include(if: $withPosts) {
                    title
                  }
                }
                """,
                """
                query  GetUser($withEmail: Boolean!) {
                  user {
                    id
                    name
                    email ~~>@include(if: $withEmail)
                    profile ~~>@include(if: true) {
                      bio
                      avatar
                    }
                  }
                }
                
                fragment UserFields on User {
                  posts ~~>@include(if: $withPosts) {
                    title
                  }
                }
                """
            )
        );
    }

    @Test
    void findDirectiveOnFragments() {
        rewriteRun(
            spec -> spec.recipe(new FindDirectiveUsages("cached")),
            graphQl(
                """
                fragment UserInfo on User @cached(ttl: 300) {
                  id
                  name
                  email
                }
                
                query {
                  user {
                    ...UserInfo
                    posts @cached {
                      title
                    }
                  }
                }
                """,
                """
                fragment UserInfo on User ~~>@cached(ttl: 300) {
                  id
                  name
                  email
                }
                
                query {
                  user {
                    ...UserInfo
                    posts ~~>@cached {
                      title
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void findMultipleDirectivesOnSameElement() {
        rewriteRun(
            graphQl(
                """
                type User {
                  email: String! @deprecated(reason: "Use emailAddress") @unique
                  legacyId: ID @deprecated @external
                }
                
                enum Status {
                  ACTIVE
                  INACTIVE @deprecated(reason: "Use ARCHIVED")
                  ARCHIVED
                }
                """,
                """
                type User {
                  email: String! ~~>@deprecated(reason: "Use emailAddress") @unique
                  legacyId: ID ~~>@deprecated @external
                }
                
                enum Status {
                  ACTIVE
                  INACTIVE ~~>@deprecated(reason: "Use ARCHIVED")
                  ARCHIVED
                }
                """
            )
        );
    }

    @Test
    void noMatchWhenDirectiveNotFound() {
        rewriteRun(
            spec -> spec.recipe(new FindDirectiveUsages("nonExistentDirective")),
            graphQl(
                """
                type User {
                  id: ID!
                  name: String! @deprecated
                }
                
                directive @auth on FIELD_DEFINITION
                
                type Query {
                  user: User @auth
                }
                """
            )
        );
    }

    @Test
    void findSkipDirective() {
        rewriteRun(
            spec -> spec.recipe(new FindDirectiveUsages("skip")),
            graphQl(
                """
                query GetUser($excludeProfile: Boolean!) {
                  user {
                    id
                    name
                    profile @skip(if: $excludeProfile) {
                      bio
                    }
                    posts @skip(if: false) {
                      title
                    }
                  }
                }
                """,
                """
                query GetUser($excludeProfile: Boolean!) {
                  user {
                    id
                    name
                    profile ~~>@skip(if: $excludeProfile) {
                      bio
                    }
                    posts ~~>@skip(if: false) {
                      title
                    }
                  }
                }
                """
            )
        );
    }
}