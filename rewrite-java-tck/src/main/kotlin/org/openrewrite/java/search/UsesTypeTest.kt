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
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RewriteTest

interface UsesTypeTest : RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2427")
    @Test
    fun primitiveTypes() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe { UsesType("double") })
        },
        java("""
            class Test {
                double d = 1d;
            }
            """,
            """
            /*~~>*/class Test {
                double d = 1d;
            }
            """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1169")
    @Test
    fun emptyConstructor() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{ UsesType("java.util.ArrayList") })
        },
        java("""
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                List<String> l = new ArrayList<>();
            }
        """,
        """
            /*~~>*/import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                List<String> l = new ArrayList<>();
            }
        """)
    )

    @Test
    fun usesTypeFindsImports() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{ UsesType("java.util.Collections") })
        },
        java("""
            import java.io.File;
            import java.util.Collections;
            
            class Test {
            }
        """,
        """
            /*~~>*/import java.io.File;
            import java.util.Collections;
            
            class Test {
            }
        """)
    )

    /**
     * Type wildcards are greedy.
     */
    @Test
    fun usesTypeWildcardFindsImports() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{ UsesType("java.util.*") })
        },
        java("""
            import java.io.File;
            import static java.util.Collections.singleton;
            
            class Test {
            }
        """,
        """
            /*~~>*/import java.io.File;
            import static java.util.Collections.singleton;
            
            class Test {
            }
        """)
    )

    @Test
    fun usesFullyQualifiedReference() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{ UsesType("java.util.*") })
        },
        java("""
            import java.util.Set;
            class Test {
                void test() {
                    Set<String> s = java.util.Collections.singleton("test");
                }
            }
        """,
        """
            /*~~>*/import java.util.Set;
            class Test {
                void test() {
                    Set<String> s = java.util.Collections.singleton("test");
                }
            }
        """)
    )

    @Test
    fun usesTypeFindsInheritedTypes() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{ UsesType("java.util.Collection") })
        },
        java("""
            import java.util.List;
            
            class Test {
            }
        """,
        """
            /*~~>*/import java.util.List;
            
            class Test {
            }
        """)
    )
}
