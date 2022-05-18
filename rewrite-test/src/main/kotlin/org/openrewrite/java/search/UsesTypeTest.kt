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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface UsesTypeTest : JavaRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1169")
    @Test
    fun emptyConstructor(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesType("java.util.ArrayList")
        },
        before = """
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                List<String> l = new ArrayList<>();
            }
        """,
        after = """
            /*~~>*/import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                List<String> l = new ArrayList<>();
            }
        """
    )

    @Test
    fun usesTypeFindsImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesType("java.util.Collections")
        },
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
        recipe = toRecipe {
            UsesType("java.util.*")
        },
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
        recipe = toRecipe {
            UsesType("java.util.*")
        },
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

    @Test
    fun usesTypeFindsInheritedTypes(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            UsesType("java.util.Collection")
        },
        before = """
            import java.util.List;
            
            class Test {
            }
        """,
        after = """
            /*~~>*/import java.util.List;
            
            class Test {
            }
        """
    )
}
