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
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface UseJavaInterfaceTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UseJavaInterface()

    @Test
    fun noTargetInUse() = assertUnchanged(
        before = """
            import java.util.Collections;
            import java.util.List;
            
            class Test {
                List<Integer> method() {
                    return Collections.emptyList();
                }
            }
        """
    )

    @Test
    fun methodIsNotPublic() = assertUnchanged(
        before = """
            import java.util.LinkedList;
            
            class Test {
                LinkedList<Integer> method() {
                    return new LinkedList<>();
                }
            }
        """
    )

    @Test
    fun returnIsAlreadyInterface() = assertUnchanged(
        before = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List<Integer> method() {
                    return new LinkedList<>();
                }
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun rawReturnType() = assertChanged(
        before = """
            import java.util.LinkedList;
            
            class Test {
                public LinkedList method() {
                    return new LinkedList<>();
                }
            }
        """,
        after = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List method() {
                    return new LinkedList<>();
                }
            }
        """
    )

    @Test
    fun parameterizedReturnType() = assertChanged(
        before = """
            import java.util.LinkedList;
            
            class Test {
                public LinkedList<Integer> method() {
                    return new LinkedList<>();
                }
            }
        """,
        after = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List<Integer> method() {
                    return new LinkedList<>();
                }
            }
        """
    )

    @Test
    fun preserveParameters() = assertChanged(
        before = """
            import java.util.LinkedList;
            
            class Test {
                public LinkedList<Integer> method(int primitive, Integer integer) {
                    return new LinkedList<>();
                }
            }
        """,
        after = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List<Integer> method(int primitive, Integer integer) {
                    return new LinkedList<>();
                }
            }
        """
    )

    @Test
    fun fieldIsNotPublic() = assertUnchanged(
        before = """
            import java.util.LinkedList;
            
            class Test {
                LinkedList<Integer> l = new LinkedList<>();
            }
        """
    )

    @Test
    fun fieldIsAlreadyInterface() = assertUnchanged(
        before = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List<Integer> l = new LinkedList<>();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun rawFieldType() = assertChanged(
        before = """
            import java.util.LinkedList;
            
            class Test {
                public LinkedList l = new LinkedList();
            }
        """,
        after = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List l = new LinkedList();
            }
        """
    )

    @Test
    fun parameterizedFieldType() = assertChanged(
        before = """
            import java.util.LinkedList;
            
            class Test {
                public LinkedList<Integer> l = new LinkedList<>();
            }
        """,
        after = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List<Integer> l = new LinkedList<>();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun arrayDeque() = assertChanged(
        before = """
            import java.util.ArrayDeque;
            
            class Test {
                public ArrayDeque d = new ArrayDeque();
            }
        """,
        after = """
            import java.util.ArrayDeque;
            import java.util.Deque;
            
            class Test {
                public Deque d = new ArrayDeque();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentLinkedDeque() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentLinkedDeque;
            
            class Test {
                public ConcurrentLinkedDeque l = new ConcurrentLinkedDeque();
            }
        """,
        after = """
            import java.util.Deque;
            import java.util.concurrent.ConcurrentLinkedDeque;
            
            class Test {
                public Deque l = new ConcurrentLinkedDeque();
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
                public AbstractList l = new ArrayList();
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                public List l = new ArrayList();
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
                public AbstractSequentialList l = new LinkedList();
            }
        """,
        after = """
            import java.util.LinkedList;
            import java.util.List;
            
            class Test {
                public List l = new LinkedList();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun arrayList() = assertChanged(
        before = """
            import java.util.ArrayList;
            
            class Test {
                public ArrayList l = new ArrayList();
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.List;
            
            class Test {
                public List l = new ArrayList();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun copyOnWriteArrayList() = assertChanged(
        before = """
            import java.util.concurrent.CopyOnWriteArrayList;
            
            class Test {
                public CopyOnWriteArrayList l = new CopyOnWriteArrayList();
            }
        """,
        after = """
            import java.util.List;
            import java.util.concurrent.CopyOnWriteArrayList;
            
            class Test {
                public List l = new CopyOnWriteArrayList();
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
                public AbstractMap m = new HashMap();
            }
        """,
        after = """
            import java.util.HashMap;
            import java.util.Map;
            
            class Test {
                public Map m = new HashMap();
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
                public EnumMap m = new EnumMap(A.class);
            }
        """,
        after = """
            import java.util.EnumMap;
            import java.util.Map;
            
            class Test {
                @SuppressWarnings("unchecked")
                public Map m = new EnumMap(A.class);
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun hashMap() = assertChanged(
        before = """
            import java.util.HashMap;
            
            class Test {
                public HashMap m = new HashMap();
            }
        """,
        after = """
            import java.util.HashMap;
            import java.util.Map;
            
            class Test {
                public Map m = new HashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun hashtable() = assertChanged(
        before = """
            import java.util.Hashtable;
            
            class Test {
                public Hashtable m = new Hashtable();
            }
        """,
        after = """
            import java.util.Hashtable;
            import java.util.Map;
            
            class Test {
                public Map m = new Hashtable();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun identityHashMap() = assertChanged(
        before = """
            import java.util.IdentityHashMap;
            
            class Test {
                public IdentityHashMap m = new IdentityHashMap();
            }
        """,
        after = """
            import java.util.IdentityHashMap;
            import java.util.Map;
            
            class Test {
                public Map m = new IdentityHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun linkedHashMap() = assertChanged(
        before = """
            import java.util.LinkedHashMap;
            
            class Test {
                public LinkedHashMap m = new LinkedHashMap();
            }
        """,
        after = """
            import java.util.LinkedHashMap;
            import java.util.Map;
            
            class Test {
                public Map m = new LinkedHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun weakHashMap() = assertChanged(
        before = """
            import java.util.WeakHashMap;
            
            class Test {
                public WeakHashMap m = new WeakHashMap();
            }
        """,
        after = """
            import java.util.Map;
            import java.util.WeakHashMap;
            
            class Test {
                public Map m = new WeakHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentHashMap() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentHashMap;
            
            class Test {
                public ConcurrentHashMap m = new ConcurrentHashMap();
            }
        """,
        after = """
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.ConcurrentMap;
            
            class Test {
                public ConcurrentMap m = new ConcurrentHashMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentSkipListMap() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentSkipListMap;
            
            class Test {
                public ConcurrentSkipListMap m = new ConcurrentSkipListMap();
            }
        """,
        after = """
            import java.util.concurrent.ConcurrentMap;
            import java.util.concurrent.ConcurrentSkipListMap;
            
            class Test {
                public ConcurrentMap m = new ConcurrentSkipListMap();
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
                public AbstractQueue q = new PriorityQueue();
            }
        """,
        after = """
            import java.util.PriorityQueue;
            import java.util.Queue;
            
            class Test {
                public Queue q = new PriorityQueue();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun concurrentLinkedQueue() = assertChanged(
        before = """
            import java.util.concurrent.ConcurrentLinkedQueue;
            
            class Test {
                public ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
            }
        """,
        after = """
            import java.util.Queue;
            import java.util.concurrent.ConcurrentLinkedQueue;
            
            class Test {
                public Queue q = new ConcurrentLinkedQueue();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun synchronousQueue() = assertChanged(
        before = """
            import java.util.concurrent.SynchronousQueue;
            
            class Test {
                public SynchronousQueue q = new SynchronousQueue();
            }
        """,
        after = """
            import java.util.Queue;
            import java.util.concurrent.SynchronousQueue;
            
            class Test {
                public Queue q = new SynchronousQueue();
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
                public AbstractSet s = new HashSet();
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set s = new HashSet();
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
                public EnumSet s = EnumSet.allOf(A.class);
            }
        """,
        after = """
            import java.util.EnumSet;
            import java.util.Set;
            
            class Test {
                public Set s = EnumSet.allOf(A.class);
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun hashSet() = assertChanged(
        before = """
            import java.util.HashSet;
            
            class Test {
                public HashSet s = new HashSet();
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                public Set s = new HashSet();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun linkedHashSet() = assertChanged(
        before = """
            import java.util.LinkedHashSet;
            
            class Test {
                public LinkedHashSet s = new LinkedHashSet();
            }
        """,
        after = """
            import java.util.LinkedHashSet;
            import java.util.Set;
            
            class Test {
                public Set s = new LinkedHashSet();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun copyOnWriteArraySet() = assertChanged(
        before = """
            import java.util.concurrent.CopyOnWriteArraySet;
            
            class Test {
                public CopyOnWriteArraySet s = new CopyOnWriteArraySet();
            }
        """,
        after = """
            import java.util.Set;
            import java.util.concurrent.CopyOnWriteArraySet;
            
            class Test {
                public Set s = new CopyOnWriteArraySet();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun treeMap() = assertChanged(
        before = """
            import java.util.TreeMap;
            
            class Test {
                public TreeMap m = new TreeMap();
            }
        """,
        after = """
            import java.util.SortedMap;
            import java.util.TreeMap;
            
            class Test {
                public SortedMap m = new TreeMap();
            }
        """
    )

    @Suppress("rawtypes")
    @Test
    fun treeSet() = assertChanged(
        before = """
            import java.util.TreeSet;
            
            class Test {
                public TreeSet s = new TreeSet();
            }
        """,
        after = """
            import java.util.SortedSet;
            import java.util.TreeSet;
            
            class Test {
                public SortedSet s = new TreeSet();
            }
        """
    )
}
