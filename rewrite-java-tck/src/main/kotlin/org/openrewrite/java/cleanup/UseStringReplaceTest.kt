/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface UseStringReplaceTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = UseStringReplace()


    @Suppress("ReplaceOnLiteralHasNoEffect")
    @Issue("https://github.com/openrewrite/rewrite/issues/2222")
    @Test
    fun literalValueSourceAccountsForEscapeCharacters() = assertChanged(
        before = """
            class A {
                String s = "".replaceAll("\n","\r\n");
            }
        """,
        after = """
            class A {
                String s = "".replace("\n","\r\n");
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1781")
    @Test
    fun replaceAllContainsEscapedQuotes() = assertChanged(
        before = """
            class Test {
                public String method(String input) {
                    return input.replaceAll("\"test\"\"", "");
                }
            }
        """,
        after = """
            class Test {
                public String method(String input) {
                    return input.replace("\"test\"\"", "");
                }
            }
        """
    )

    @Test
    @DisplayName("String#repalaceAll replaced by String#replace, 'cause fist argument is not a regular expression")
    fun replaceAllReplacedByReplace() = assertChanged(
            before = """
                class Test {
                    public void method() {
                        String someText = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                        String newText = someText.replaceAll("Bob is", "It's");
                    }
                }
            """,
            after = """
                class Test {
                    public void method() {
                        String someText = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                        String newText = someText.replace("Bob is", "It's");
                    }
                }
            """
    )

    @Test
    @DisplayName("String#repalaceAll replaced by String#replace, 'cause fist argument is not a regular expression besides it contains special characters")
    fun replaceAllReplacedByReplaceWithSpecialCharacters() = assertChanged(
            before = """
                class Test {
                    public void method() {
                        String someText = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                        String newText = someText.replaceAll("Bob is\\.", "It's");
                    }
                }
            """,
            after = """
                class Test {
                    public void method() {
                        String someText = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                        String newText = someText.replace("Bob is.", "It's");
                    }
                }
            """
    )

    @Test
    @DisplayName("String#repalaceAll is not replaced by String#replace, 'cause fist argument is a real regular expression")
    fun replaceAllUnchanged() = assertUnchanged(
            before = """
                class Test {
                    public void method() {
                        String someText = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                        String newText = someText.replaceAll("\\w*\\sis", "It's");
                    }
                }
            """
    )
}
