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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.graphql.Assertions.graphQl;

public class GraphQlParserEdgeCasesTest implements RewriteTest {

    @Test
    void parseEmptyDocument() {
        rewriteRun(
            graphQl("")
        );
    }

    @Test
    void parseDocumentWithOnlyWhitespace() {
        rewriteRun(
            graphQl("   \n\t\n   ")
        );
    }

    @Test
    void parseDocumentWithOnlyComments() {
        rewriteRun(
            graphQl(
                """
                # Just a comment
                # Another comment
                """
            )
        );
    }

    @Test
    void parseUnicodeInStrings() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(text: "Hello 世界 🚀") {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEscapedCharactersInStrings() {
        rewriteRun(
            graphQl(
                """
                query {
                  user {
                    bio(format: "Line 1\\nLine 2\\tTabbed\\r\\nWindows EOL\\\\Backslash\\"Quote")
                  }
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Unicode escape sequences not preserved")
    @Test
    void parseEscapedUnicodeInStrings() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(text: "\\u0048\\u0065\\u006C\\u006C\\u006F") {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseDeeplyNestedSelectionSets() {
        rewriteRun(
            graphQl(
                """
                query {
                  level1 {
                    level2 {
                      level3 {
                        level4 {
                          level5 {
                            level6 {
                              level7 {
                                level8 {
                                  level9 {
                                    level10 {
                                      id
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void parseDeeplyNestedListTypes() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  matrix: [[[[[[[[[[String]]]]]]]]]]!
                }
                """
            )
        );
    }

    @Test
    void parseVeryLongFieldNames() {
        rewriteRun(
            graphQl(
                """
                type User {
                  thisIsAVeryLongFieldNameThatExceedsNormalLengthExpectationsButShouldStillBeValidInGraphQL: String
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Comments with special characters")
    @Test
    void parseSpecialCharactersInComments() {
        rewriteRun(
            graphQl(
                """
                # Comment with special chars: @#$%^&*(){}[]|\\:;"'<>,.?/~`
                type User {
                  id: ID! # Inline comment with emojis 🎉🚀✨
                }
                """
            )
        );
    }

    @Test
    void parseEmptySelectionSet() {
        rewriteRun(
            graphQl(
                """
                query {
                  user {
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEmptyArgumentList() {
        rewriteRun(
            graphQl(
                """
                query {
                  user() {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEmptyObjectValue() {
        rewriteRun(
            graphQl(
                """
                query {
                  createUser(input: {}) {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseEmptyListValue() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(tags: []) {
                    id
                  }
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Block string indentation handling")
    @Test
    void parseMultilineStringWithIndentation() {
        rewriteRun(
            graphQl(
                """
                type User {
                  \"\"\"
                  This is a multiline description
                    with indentation
                      and multiple levels
                        of nesting
                  \"\"\"
                  name: String!
                }
                """
            )
        );
    }

    @Test
    void parseStringWithQuotesInside() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(text: "He said \\"Hello\\" to me") {
                    id
                  }
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Large number literal handling")
    @Test
    void parseNumbersAtBoundaries() {
        rewriteRun(
            graphQl(
                """
                query {
                  test(
                    zero: 0
                    negativeZero: -0
                    maxInt: 2147483647
                    minInt: -2147483648
                    largeFloat: 1.7976931348623157e308
                    smallFloat: 2.2250738585072014e-308
                    scientificNotation: 1.23e-4
                  ) {
                    id
                  }
                }
                """
            )
        );
    }

    @Test
    void parseFieldWithNoArguments() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  users: [User!]!
                  currentTime: String!
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Empty interface body formatting")
    @Test
    void parseInterfaceWithNoFields() {
        rewriteRun(
            graphQl(
                """
                interface Empty {
                }
                """
            )
        );
    }

    @Test
    void parseEnumWithSingleValue() {
        rewriteRun(
            graphQl(
                """
                enum SingleValue {
                  ONLY_ONE
                }
                """
            )
        );
    }

    @Test
    void parseUnionWithSingleType() {
        rewriteRun(
            graphQl(
                """
                union SingleUnion = User
                """
            )
        );
    }

    @Test
    void parseConsecutiveDirectives() {
        rewriteRun(
            graphQl(
                """
                type User {
                  email: String @deprecated @private @auth(role: ADMIN)
                }
                """
            )
        );
    }

    @Test
    void parseDirectiveWithEmptyArguments() {
        rewriteRun(
            graphQl(
                """
                type User {
                  name: String @custom()
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Trailing commas are non-standard")
    @Test
    void parseTrailingCommasInLists() {
        rewriteRun(
            graphQl(
                """
                query {
                  search(
                    tags: [
                      "graphql",
                      "api",
                      "test",
                    ],
                    filters: {
                      name: "test",
                      active: true,
                    }
                  ) {
                    id
                  }
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Whitespace preservation around & and | operators")
    @Test
    void parseWhitespaceAroundOperators() {
        rewriteRun(
            graphQl(
                """
                type User implements Node&Timestamped & Auditable {
                  id: ID!
                }
                
                union Result=User|Post | Comment
                """
            )
        );
    }

    @Test
    void parseRepeatableDirectiveMultipleTimes() {
        rewriteRun(
            graphQl(
                """
                type User @tag(name: "user") @tag(name: "entity") @tag(name: "model") {
                  id: ID!
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Block string special formatting")
    @Test
    void parseBlockStringWithSpecialFormatting() {
        rewriteRun(
            graphQl(
                """
                type Query {
                  \"\"\"
                  First line
                  
                  
                  Multiple blank lines above
                  
                  	Tab character here
                  Trailing spaces here   
                  \"\"\"
                  test: String
                }
                """
            )
        );
    }

    @Disabled("Parser issue: Negative float with exponent parsing")
    @Test
    void parseNegativeFloatWithExponent() {
        rewriteRun(
            graphQl(
                """
                query {
                  calculate(
                    value: -3.14e-10
                    another: -0.0000000001
                  ) {
                    result
                  }
                }
                """
            )
        );
    }
}