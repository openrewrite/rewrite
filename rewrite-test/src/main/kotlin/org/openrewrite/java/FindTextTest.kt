/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.config.Environment
import org.openrewrite.java.search.FindText

interface FindTextTest : JavaRecipeTest {

    @Test
    fun findText(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindText(
            listOf("test", "12.*")
        ),
        before = """
            // not this one
            // test
            // not this one, either
            // comment 123
            class Test {
                int n = 123;
                String s = "test";
                String s = "mytest";
            }
        """,
        after = """
            // not this one
            /*~~>*/// test
            // not this one, either
            /*~~>*/// comment 123
            class Test {
                int n = /*~~>*/123;
                String s = /*~~>*/"test";
                String s = /*~~>*/"mytest";
            }
        """
    )

    @Test
    fun findSecrets(jp: JavaParser) = assertChanged(
        jp,
        recipe = Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.java.search.FindSecrets"),
        before = """
            class Test {
                String uhOh = "-----BEGIN RSA PRIVATE KEY-----";
            }
        """,
        after = """
            class Test {
                String uhOh = /*~~>*/"-----BEGIN RSA PRIVATE KEY-----";
            }
        """
    )
}
