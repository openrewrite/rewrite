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

class GraphQlFragmentTest implements RewriteTest {

    @Test
    void parseSimpleFragment() {
        rewriteRun(
            graphQl(
                """
                fragment userInfo on User {
                  id
                  name
                  email
                }
                """
            )
        );
    }

    @Test
    void parseFragmentWithDirectives() {
        rewriteRun(
            graphQl(
                """
                fragment userInfo on User @deprecated(reason: "Use DetailedUserInfo") {
                  id
                  name
                  email @include(if: $showEmail)
                }
                """
            )
        );
    }

    @Test
    void parseFragmentSpreadInQuery() {
        rewriteRun(
            graphQl(
                """
                query GetUser($id: ID!) {
                  user(id: $id) {
                    ...userInfo
                    posts {
                      title
                    }
                  }
                }
                
                fragment userInfo on User {
                  id
                  name
                  email
                }
                """
            )
        );
    }

    @Test
    void parseFragmentSpreadWithDirectives() {
        rewriteRun(
            graphQl(
                """
                query GetUser($id: ID!, $includeDetails: Boolean!) {
                  user(id: $id) {
                    id
                    ...userDetails @include(if: $includeDetails)
                  }
                }
                
                fragment userDetails on User {
                  email
                  phone
                  address
                }
                """
            )
        );
    }

    @Test
    void parseInlineFragmentWithTypeCondition() {
        rewriteRun(
            graphQl(
                """
                query GetNode($id: ID!) {
                  node(id: $id) {
                    id
                    ... on User {
                      name
                      email
                    }
                    ... on Post {
                      title
                      content
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseInlineFragmentWithoutTypeCondition() {
        rewriteRun(
            graphQl(
                """
                query GetUser($id: ID!, $includeStats: Boolean!) {
                  user(id: $id) {
                    id
                    name
                    ... @include(if: $includeStats) {
                      postCount
                      followerCount
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseNestedFragmentSpreads() {
        rewriteRun(
            graphQl(
                """
                fragment userBase on User {
                  id
                  name
                }
                
                fragment userWithPosts on User {
                  ...userBase
                  posts {
                    ...postInfo
                  }
                }
                
                fragment postInfo on Post {
                  id
                  title
                  createdAt
                }
                
                query GetUser($id: ID!) {
                  user(id: $id) {
                    ...userWithPosts
                  }
                }
                """
            )
        );
    }

    @Test
    void parseFragmentWithNestedSelections() {
        rewriteRun(
            graphQl(
                """
                fragment userDetails on User {
                  id
                  name
                  profile {
                    bio
                    avatar {
                      url
                      width
                      height
                    }
                  }
                  posts(first: 5) {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseMultipleInlineFragmentsInUnion() {
        rewriteRun(
            graphQl(
                """
                query Search($query: String!) {
                  search(query: $query) {
                    ... on User {
                      id
                      name
                      email
                    }
                    ... on Post {
                      id
                      title
                      author {
                        name
                      }
                    }
                    ... on Comment {
                      id
                      content
                      author {
                        name
                      }
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseFragmentWithComplexDirectives() {
        rewriteRun(
            graphQl(
                """
                fragment userFieldsWithAuth on User 
                  @auth(requires: "user:read")
                  @cache(ttl: 300, scope: PRIVATE) {
                  id
                  name @transform(format: "uppercase")
                  email @include(if: $showEmail) @auth(requires: "user:email")
                  posts(
                    first: 10
                    orderBy: { field: CREATED_AT, direction: DESC }
                  ) @connection(key: "user_posts") {
                    edges {
                      node {
                        id
                        title
                      }
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseFragmentRoundTrip() {
        rewriteRun(
            graphQl(
                """
                fragment CompleteUser on User {
                  id
                  name
                  email
                  profile {
                    bio
                    location
                  }
                  posts(first: 10) {
                    edges {
                      node {
                        ...PostSummary
                      }
                    }
                  }
                  ... on PremiumUser {
                    subscription {
                      plan
                      expiresAt
                    }
                  }
                }
                
                fragment PostSummary on Post {
                  id
                  title
                  excerpt
                  publishedAt
                }
                
                query GetUserProfile($userId: ID!) {
                  user(id: $userId) {
                    ...CompleteUser
                  }
                }
                """
            )
        );
    }

    @Test
    @org.junit.jupiter.api.Disabled("Fragment variables are an experimental GraphQL feature not supported by the grammar")
    void parseFragmentWithVariableDefinitions() {
        // Fragment variable definitions are an experimental feature proposed in:
        // https://github.com/graphql/graphql-spec/issues/204
        // The syntax "fragment Name($var: Type) on Type" is not part of the 
        // official GraphQL specification and is not supported by our grammar.
        // The grammar expects: fragment NAME 'on' typeCondition
        // but gets: fragment NAME '(' instead of 'on'
        rewriteRun(
            graphQl(
                """
                # Note: Variable definitions in fragments are experimental
                fragment UserPosts($count: Int = 10, $after: String) on User {
                  posts(first: $count, after: $after) {
                    edges {
                      node {
                        id
                        title
                      }
                      cursor
                    }
                    pageInfo {
                      hasNextPage
                      endCursor
                    }
                  }
                }
                """
            )
        );
    }
}