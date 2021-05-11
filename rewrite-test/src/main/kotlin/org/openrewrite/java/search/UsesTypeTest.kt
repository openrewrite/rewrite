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
package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface UsesTypeTest : JavaRecipeTest {
    @Test
    fun usesTypeFindsImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = UsesType<ExecutionContext>("java.util.Collections")
            .toRecipe(),
        before = """
            import java.io.File;
            import java.util.Collections;
            
            class Test {
            }
        """,
        after = """
            /*~~>*/import java.io.File;
            import java.util.Collections;
            
            class Test {
            }
        """
    )

    /**
     * Type wildcards are greedy.
     */
    @Test
    fun usesTypeWildcardFindsImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = UsesType<ExecutionContext>("java.util.*").toRecipe(),
        before = """
            import java.io.File;
            import static java.util.Collections.singleton;
            
            class Test {
            }
        """,
        after = """
            /*~~>*/import java.io.File;
            import static java.util.Collections.singleton;
            
            class Test {
            }
        """
    )

    @Test
    fun usesFullyQualifiedReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = UsesType<ExecutionContext>("java.util.*").toRecipe(),
        before = """
            import java.util.Set;
            class Test {
                void test() {
                    Set<String> s = java.util.Collections.singleton("test");
                }
            }
        """,
        after = """
            /*~~>*/import java.util.Set;
            class Test {
                void test() {
                    Set<String> s = java.util.Collections.singleton("test");
                }
            }
        """
    )
}
