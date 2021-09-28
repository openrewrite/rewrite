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
@file:Suppress("unchecked")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.marker.JavaVersion
import org.openrewrite.java.search.UsesJavaVersion
import org.openrewrite.java.tree.J
import java.util.*

interface NoDoubleBraceInitializationTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = NoDoubleBraceInitialization()

    @Test
    fun doubleBranchInitializationForArg() = assertUnchanged(
        before = """
            import java.util.HashMap;
            import java.util.Map;
            class A {
                void m1(Map<String, String> map) {
                }
                void m2() {
                    m1(new HashMap<String, String>(){{put("a", "a");}});
                }
            }
        """
    )

    @Test
    fun doubleBraceInitializationForFieldVar() = assertChanged(
        before = """
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            
            class A {
                private static final Map<String, String> map = new HashMap() {{put("a", "a");}};
                private final List<String> lst = new ArrayList() {{add("x");add("y");}};
                private final Set<String> mySet = new HashSet(){{add("q");}};
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;
            
            class A {
                private static final Map<String, String> map;
                static {
                    map = new HashMap<>();
                    map.put("a", "a");
                }
                private final List<String> lst;
                {
                    lst = new ArrayList<>();
                    lst.add("x");
                    lst.add("y");
                }
                private final Set<String> mySet;
                {
                    mySet = new HashSet<>();
                    mySet.add("q");
                }
            }
        """
    )

    @Test
    fun memberVar() = assertChanged(
        before = """
            import java.util.HashMap;
            import java.util.Map;
            class A {
                void example() {
                    Map<String, String> aMap = new HashMap<>();
                    aMap.put("c", "c");
                    String s = "x";
                    Map<String, String> bMap = new HashMap(){{
                        s.concat("z");
                        put("a", "A");
                        put("b", "B");
                    }};
                }
            }
        """,
        after = """
            import java.util.HashMap;
            import java.util.Map;
            class A {
                void example() {
                    Map<String, String> aMap = new HashMap<>();
                    aMap.put("c", "c");
                    String s = "x";
                    Map<String, String> bMap = new HashMap();
                    s.concat("z");
                    bMap.put("a", "A");
                    bMap.put("b", "B");
                }
            }
        """
    )
}
