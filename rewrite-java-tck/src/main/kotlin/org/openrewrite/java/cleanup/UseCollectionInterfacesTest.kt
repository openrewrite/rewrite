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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.marker.JavaVersion
import java.util.*

interface UseCollectionInterfacesTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UseCollectionInterfaces()

    @Test
    fun noTargetInUse() = assertUnchanged(
        before = """
            import java.util.Collections;
            import java.util.Set;
            
            class Test {
                Set<Integer> method() {
                    return Collections.emptySet();
                }
            }
        """
    )

    @Test
    fun returnIsAlreadyInterface() = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set<Integer> method() {
                    return new HashSet<>();
                }
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun rawReturnType() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                public HashSet method() {
                    return new HashSet<>();
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set method() {
                    return new HashSet<>();
                }
            }
        """
    )

    @Test
    fun parameterizedReturnType() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                public HashSet<Integer> method() {
                    return new HashSet<>();
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set<Integer> method() {
                    return new HashSet<>();
                }
            }
        """
    )

    @Test
    fun preserveParameters() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                public HashSet<Integer> method(int primitive, Integer integer) {
                    return new HashSet<>();
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set<Integer> method(int primitive, Integer integer) {
                    return new HashSet<>();
                }
            }
        """
    )

    @Test
    fun fieldIsAlreadyInterface() = assertUnchanged(
        before = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set<Integer> values = new HashSet<>();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun rawFieldType() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                public HashSet values = new HashSet();
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set values = new HashSet();
            }
        """
    )

    @Test
    fun parameterizedFieldType() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                public HashSet<Integer> values = new HashSet<>();
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set<Integer> values = new HashSet<>();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun arrayDeque() = assertChanged(
        before = """
            import java.util.ArrayDeque;
            
            class Test {
                public ArrayDeque values = new ArrayDeque();
            }
        """,
        after = """
            import java.util.ArrayDeque;
            import java.util.Deque;
            
            class Test {
                public Deque values = new ArrayDeque();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentLinkedDeque() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentLinkedDeque;
            
            class Test {
                public ConcurrentLinkedDeque values = new ConcurrentLinkedDeque();
            }
        """,
        after = """
            import java.util.Deque;
            import java.util.concurrent.ConcurrentLinkedDeque;
            
            class Test {
                public Deque values = new ConcurrentLinkedDeque();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun abstractList() = assertChanged(
        before = """
            import java.util.ArrayList;
            import java.util.AbstractList;
            
            class Test {
                public AbstractList values = new ArrayList();
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                public List values = new ArrayList();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun abstractSequentialList() = assertChanged(
        before = """
            import java.util.AbstractSequentialList;
            import java.util.LinkedList;
            
            class Test {
                public AbstractSequentialList values = new LinkedList();
            }
        """,
        after = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List values = new LinkedList();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun arrayList() = assertChanged(
        before = """
            import java.util.ArrayList;
            
            class Test {
                public ArrayList values = new ArrayList();
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                public List values = new ArrayList();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun copyOnWriteArrayList() = assertChanged(
        before = """
            import java.util.concurrent.CopyOnWriteArrayList;
            
            class Test {
                public CopyOnWriteArrayList values = new CopyOnWriteArrayList();
            }
        """,
        after = """
            import java.util.List;
            import java.util.concurrent.CopyOnWriteArrayList;
            
            class Test {
                public List values = new CopyOnWriteArrayList();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun abstractMap() = assertChanged(
        before = """
            import java.util.AbstractMap;
            import java.util.HashMap;
            
            class Test {
                public AbstractMap values = new HashMap();
            }
        """,
        after = """
            import java.util.HashMap;
            import java.util.Map;
            
            class Test {
                public Map values = new HashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun enumMap() = assertChanged(
        dependsOn = arrayOf("""
            public enum A {}
        """),
        before = """
            import java.util.EnumMap;
            
            class Test {
                @SuppressWarnings("unchecked")
                public EnumMap values = new EnumMap(A.class);
            }
        """,
        after = """
            import java.util.EnumMap;
            import java.util.Map;
            
            class Test {
                @SuppressWarnings("unchecked")
                public Map values = new EnumMap(A.class);
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun hashMap() = assertChanged(
        before = """
            import java.util.HashMap;
            
            class Test {
                public HashMap values = new HashMap();
            }
        """,
        after = """
            import java.util.HashMap;
            import java.util.Map;
            
            class Test {
                public Map values = new HashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun hashtable() = assertChanged(
        before = """
            import java.util.Hashtable;
            
            class Test {
                public Hashtable values = new Hashtable();
            }
        """,
        after = """
            import java.util.Hashtable;
            import java.util.Map;
            
            class Test {
                public Map values = new Hashtable();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun identityHashMap() = assertChanged(
        before = """
            import java.util.IdentityHashMap;
            
            class Test {
                public IdentityHashMap values = new IdentityHashMap();
            }
        """,
        after = """
            import java.util.IdentityHashMap;
            import java.util.Map;
            
            class Test {
                public Map values = new IdentityHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun linkedHashMap() = assertChanged(
        before = """
            import java.util.LinkedHashMap;
            
            class Test {
                public LinkedHashMap values = new LinkedHashMap();
            }
        """,
        after = """
            import java.util.LinkedHashMap;
            import java.util.Map;
            
            class Test {
                public Map values = new LinkedHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun weakHashMap() = assertChanged(
        before = """
            import java.util.WeakHashMap;
            
            class Test {
                public WeakHashMap values = new WeakHashMap();
            }
        """,
        after = """
            import java.util.Map;
            import java.util.WeakHashMap;
            
            class Test {
                public Map values = new WeakHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentHashMap() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentHashMap;
            
            class Test {
                public ConcurrentHashMap values = new ConcurrentHashMap();
            }
        """,
        after = """
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.ConcurrentMap;
            
            class Test {
                public ConcurrentMap values = new ConcurrentHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentSkipListMap() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentSkipListMap;
            
            class Test {
                public ConcurrentSkipListMap values = new ConcurrentSkipListMap();
            }
        """,
        after = """
            import java.util.concurrent.ConcurrentMap;
            import java.util.concurrent.ConcurrentSkipListMap;
            
            class Test {
                public ConcurrentMap values = new ConcurrentSkipListMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun abstractQueue() = assertChanged(
        before = """
            import java.util.AbstractQueue;
            import java.util.PriorityQueue;
            
            class Test {
                public AbstractQueue values = new PriorityQueue();
            }
        """,
        after = """
            import java.util.PriorityQueue;
            import java.util.Queue;
            
            class Test {
                public Queue values = new PriorityQueue();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentLinkedQueue() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentLinkedQueue;
            
            class Test {
                public ConcurrentLinkedQueue values = new ConcurrentLinkedQueue();
            }
        """,
        after = """
            import java.util.Queue;
            import java.util.concurrent.ConcurrentLinkedQueue;
            
            class Test {
                public Queue values = new ConcurrentLinkedQueue();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun abstractSet() = assertChanged(
        before = """
            import java.util.AbstractSet;
            import java.util.HashSet;
            
            class Test {
                public AbstractSet values = new HashSet();
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set values = new HashSet();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun enumSet() = assertChanged(
        dependsOn = arrayOf("""
            public enum A {}
        """),
        before = """
            import java.util.EnumSet;
            
            class Test {
                public EnumSet values = EnumSet.allOf(A.class);
            }
        """,
        after = """
            import java.util.EnumSet;
            import java.util.Set;
            
            class Test {
                public Set values = EnumSet.allOf(A.class);
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun hashSet() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                public HashSet values = new HashSet();
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set values = new HashSet();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun linkedHashSet() = assertChanged(
        before = """
            import java.util.LinkedHashSet;
            
            class Test {
                public LinkedHashSet values = new LinkedHashSet();
            }
        """,
        after = """
            import java.util.LinkedHashSet;
            import java.util.Set;
            
            class Test {
                public Set values = new LinkedHashSet();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun copyOnWriteArraySet() = assertChanged(
        before = """
            import java.util.concurrent.CopyOnWriteArraySet;
            
            class Test {
                public CopyOnWriteArraySet values = new CopyOnWriteArraySet();
            }
        """,
        after = """
            import java.util.Set;
            import java.util.concurrent.CopyOnWriteArraySet;
            
            class Test {
                public Set values = new CopyOnWriteArraySet();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    fun privateVariable() = assertChanged(
        before = """
            import java.util.ArrayList;
            
            class Test {
                private ArrayList<Integer> values = new ArrayList<>();
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                private List<Integer> values = new ArrayList<>();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    fun noModifierOnVariable() = assertChanged(
        before = """
            import java.util.ArrayList;
            
            class Test {
                ArrayList<Integer> values = new ArrayList<>();
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                List<Integer> values = new ArrayList<>();
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    fun privateMethod() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                private HashSet<Integer> method() {
                    return new HashSet<>();
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                private Set<Integer> method() {
                    return new HashSet<>();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    fun noModifierOnMethod() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                HashSet<Integer> method() {
                    return new HashSet<>();
                }
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                Set<Integer> method() {
                    return new HashSet<>();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1771")
    @Test
    fun variableWithVar() {
        val javaRuntimeVersion = System.getProperty("java.runtime.version")
        val javaVendor = System.getProperty("java.vm.vendor")
        if (JavaVersion(UUID.randomUUID(), javaRuntimeVersion, javaVendor, javaRuntimeVersion, javaRuntimeVersion).majorVersion >= 10) {
            assertUnchanged(parser, recipe, executionContext,
            """
            import java.util.ArrayList;
            
            class Test {
                public void method() {
                    var list = new ArrayList<>();
                }
            }
            """.trimIndent())
        }
    }
}
