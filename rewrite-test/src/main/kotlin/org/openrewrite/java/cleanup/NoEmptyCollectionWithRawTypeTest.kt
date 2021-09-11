/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface NoEmptyCollectionWithRawTypeTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = NoEmptyCollectionWithRawType()

    @Test
    fun emptyListFullyQualified() = assertChanged(
        before = """
            import java.util.List;
            
            @SuppressWarnings("unchecked")
            class Test {
                List<Integer> l = java.util.Collections.EMPTY_LIST;
            }
        """,
        after = """
            import java.util.List;
            
            @SuppressWarnings("unchecked")
            class Test {
                List<Integer> l = java.util.Collections.emptyList();
            }
        """
    )

    @Test
    fun emptyListStaticImport() = assertChanged(
        before = """
            import java.util.List;
            
            import static java.util.Collections.EMPTY_LIST;
            
            @SuppressWarnings("unchecked")
            class Test {
                List<Integer> l = EMPTY_LIST;
            }
        """,
        after = """
            import java.util.List;
            
            import static java.util.Collections.emptyList;
            
            @SuppressWarnings("unchecked")
            class Test {
                List<Integer> l = emptyList();
            }
        """
    )

    @Test
    fun emptyListFieldAccess() = assertChanged(
        before = """
            import java.util.Collections;
            import java.util.List;
            
            @SuppressWarnings("unchecked")
            class Test {
                List<Integer> l = Collections.EMPTY_LIST;
            }
        """,
        after = """
            import java.util.Collections;
            import java.util.List;
            
            @SuppressWarnings("unchecked")
            class Test {
                List<Integer> l = Collections.emptyList();
            }
        """
    )

    @Test
    fun emptyMapFullyQualified() = assertChanged(
        before = """
            import java.util.Map;
            
            @SuppressWarnings("unchecked")
            class Test {
                Map<Integer, Integer> m = java.util.Collections.EMPTY_MAP;
            }
        """,
        after = """
            import java.util.Map;
            
            @SuppressWarnings("unchecked")
            class Test {
                Map<Integer, Integer> m = java.util.Collections.emptyMap();
            }
        """
    )

    @Test
    fun emptyMapStaticImport() = assertChanged(
        before = """
            import java.util.Map;
            
            import static java.util.Collections.EMPTY_MAP;
            
            @SuppressWarnings("unchecked")
            class Test {
                Map<Integer, Integer> l = EMPTY_MAP;
            }
        """,
        after = """
            import java.util.Map;
            
            import static java.util.Collections.emptyMap;
            
            @SuppressWarnings("unchecked")
            class Test {
                Map<Integer, Integer> l = emptyMap();
            }
        """
    )

    @Test
    fun emptyMapFieldAccess() = assertChanged(
        before = """
            import java.util.Collections;
            import java.util.Map;
            
            @SuppressWarnings("unchecked")
            class Test {
                Map<Integer, Integer> m = Collections.EMPTY_MAP;
            }
        """,
        after = """
            import java.util.Collections;
            import java.util.Map;
            
            @SuppressWarnings("unchecked")
            class Test {
                Map<Integer, Integer> m = Collections.emptyMap();
            }
        """
    )

    @Test
    fun emptySetFullyQualified() = assertChanged(
        before = """
            import java.util.Set;
            
            @SuppressWarnings("unchecked")
            class Test {
                Set<Integer> m = java.util.Collections.EMPTY_SET;
            }
        """,
        after = """
            import java.util.Set;
            
            @SuppressWarnings("unchecked")
            class Test {
                Set<Integer> m = java.util.Collections.emptySet();
            }
        """
    )

    @Test
    fun emptySetStaticImport() = assertChanged(
        before = """
            import java.util.Set;
            
            import static java.util.Collections.EMPTY_SET;
            
            @SuppressWarnings("unchecked")
            class Test {
                Set<Integer> l = EMPTY_SET;
            }
        """,
        after = """
            import java.util.Set;
            
            import static java.util.Collections.emptySet;
            
            @SuppressWarnings("unchecked")
            class Test {
                Set<Integer> l = emptySet();
            }
        """
    )

    @Test
    fun emptySetFieldAccess() = assertChanged(
        before = """
            import java.util.Collections;
            import java.util.Set;
            
            @SuppressWarnings("unchecked")
            class Test {
                Set<Integer> s = Collections.EMPTY_SET;
            }
        """,
        after = """
            import java.util.Collections;
            import java.util.Set;
            
            @SuppressWarnings("unchecked")
            class Test {
                Set<Integer> s = Collections.emptySet();
            }
        """
    )
}
