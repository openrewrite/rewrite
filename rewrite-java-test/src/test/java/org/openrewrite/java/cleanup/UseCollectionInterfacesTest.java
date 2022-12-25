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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("rawtypes")
class UseCollectionInterfacesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseCollectionInterfaces());
    }

    @Test
    void noTargetInUse() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              import java.util.Set;
              
              class Test {
                  Set<Integer> method() {
                      return Collections.emptySet();
                  }
              }
              """
          )
        );
    }

    @Test
    void returnIsAlreadyInterface() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set<Integer> method() {
                      return new HashSet<>();
                  }
              }
              """
          )
        );
    }

    @Test
    void rawReturnType() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  public HashSet method() {
                      return new HashSet<>();
                  }
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set method() {
                      return new HashSet<>();
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterizedReturnType() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  public HashSet<Integer> method() {
                      return new HashSet<>();
                  }
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set<Integer> method() {
                      return new HashSet<>();
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveParameters() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  public HashSet<Integer> method(int primitive, Integer integer) {
                      return new HashSet<>();
                  }
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set<Integer> method(int primitive, Integer integer) {
                      return new HashSet<>();
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldIsAlreadyInterface() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set<Integer> values = new HashSet<>();
              }
              """
          )
        );
    }

    @Test
    void rawFieldType() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  public HashSet values = new HashSet();
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set values = new HashSet();
              }
              """
          )
        );
    }

    @Test
    void parameterizedFieldType() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  public HashSet<Integer> values = new HashSet<>();
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set<Integer> values = new HashSet<>();
              }
              """
          )
        );
    }

    @Test
    void arrayDeque() {
        rewriteRun(
          java(
            """
              import java.util.ArrayDeque;
              
              class Test {
                  public ArrayDeque values = new ArrayDeque();
              }
              """,
            """
              import java.util.ArrayDeque;
              import java.util.Deque;
              
              class Test {
                  public Deque values = new ArrayDeque();
              }
              """
          )
        );
    }

    @Test
    void concurrentLinkedDeque() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.ConcurrentLinkedDeque;
              
              class Test {
                  public ConcurrentLinkedDeque values = new ConcurrentLinkedDeque();
              }
              """,
            """
              import java.util.Deque;
              import java.util.concurrent.ConcurrentLinkedDeque;
              
              class Test {
                  public Deque values = new ConcurrentLinkedDeque();
              }
              """
          )
        );
    }

    @Test
    void abstractList() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.AbstractList;
              
              class Test {
                  public AbstractList values = new ArrayList();
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;
              
              class Test {
                  public List values = new ArrayList();
              }
              """
          )
        );
    }

    @Test
    void abstractSequentialList() {
        rewriteRun(
          java(
            """
              import java.util.AbstractSequentialList;
              import java.util.LinkedList;
              
              class Test {
                  public AbstractSequentialList values = new LinkedList();
              }
              """,
            """
              import java.util.LinkedList;
              import java.util.List;
              
              class Test {
                  public List values = new LinkedList();
              }
              """
          )
        );
    }

    @Test
    void arrayList() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              
              class Test {
                  public ArrayList values = new ArrayList();
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;
              
              class Test {
                  public List values = new ArrayList();
              }
              """
          )
        );
    }

    @Test
    void copyOnWriteArrayList() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.CopyOnWriteArrayList;
              
              class Test {
                  public CopyOnWriteArrayList values = new CopyOnWriteArrayList();
              }
              """,
            """
              import java.util.List;
              import java.util.concurrent.CopyOnWriteArrayList;
              
              class Test {
                  public List values = new CopyOnWriteArrayList();
              }
              """
          )
        );
    }

    @Test
    void abstractMap() {
        rewriteRun(
          java(
            """
              import java.util.AbstractMap;
              import java.util.HashMap;
              
              class Test {
                  public AbstractMap values = new HashMap();
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;
              
              class Test {
                  public Map values = new HashMap();
              }
              """
          )
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void enumMap() {
        rewriteRun(
          java("public enum A {}"),
          java(
            """
              import java.util.EnumMap;
              
              class Test {
                  public EnumMap values = new EnumMap(A.class);
              }
              """,
            """
              import java.util.EnumMap;
              import java.util.Map;
              
              class Test {
                  public Map values = new EnumMap(A.class);
              }
              """
          )
        );
    }

    @Test
    void hashMap() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              
              class Test {
                  public HashMap values = new HashMap();
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;
              
              class Test {
                  public Map values = new HashMap();
              }
              """
          )
        );
    }

    @Test
    void hashtable() {
        rewriteRun(
          java(
            """
              import java.util.Hashtable;
              
              class Test {
                  public Hashtable values = new Hashtable();
              }
              """,
            """
              import java.util.Hashtable;
              import java.util.Map;
              
              class Test {
                  public Map values = new Hashtable();
              }
              """
          )
        );
    }

    @Test
    void identityHashMap() {
        rewriteRun(
          java(
            """
              import java.util.IdentityHashMap;
              
              class Test {
                  public IdentityHashMap values = new IdentityHashMap();
              }
              """,
            """
              import java.util.IdentityHashMap;
              import java.util.Map;
              
              class Test {
                  public Map values = new IdentityHashMap();
              }
              """
          )
        );
    }

    @Test
    void linkedHashMap() {
        rewriteRun(
          java(
            """
              import java.util.LinkedHashMap;
              
              class Test {
                  public LinkedHashMap values = new LinkedHashMap();
              }
              """,
            """
              import java.util.LinkedHashMap;
              import java.util.Map;
              
              class Test {
                  public Map values = new LinkedHashMap();
              }
              """
          )
        );
    }

    @Test
    void weakHashMap() {
        rewriteRun(
          java(
            """
              import java.util.WeakHashMap;
              
              class Test {
                  public WeakHashMap values = new WeakHashMap();
              }
              """,
            """
              import java.util.Map;
              import java.util.WeakHashMap;
              
              class Test {
                  public Map values = new WeakHashMap();
              }
              """
          )
        );
    }

    @Test
    void concurrentHashMap() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.ConcurrentHashMap;
              
              class Test {
                  public ConcurrentHashMap values = new ConcurrentHashMap();
              }
              """,
            """
              import java.util.concurrent.ConcurrentHashMap;
              import java.util.concurrent.ConcurrentMap;
              
              class Test {
                  public ConcurrentMap values = new ConcurrentHashMap();
              }
              """
          )
        );
    }

    @Test
    void concurrentSkipListMap() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.ConcurrentSkipListMap;
              
              class Test {
                  public ConcurrentSkipListMap values = new ConcurrentSkipListMap();
              }
              """,
            """
              import java.util.concurrent.ConcurrentMap;
              import java.util.concurrent.ConcurrentSkipListMap;
              
              class Test {
                  public ConcurrentMap values = new ConcurrentSkipListMap();
              }
              """
          )
        );
    }

    @Test
    void abstractQueue() {
        rewriteRun(
          java(
            """
              import java.util.AbstractQueue;
              import java.util.PriorityQueue;
              
              class Test {
                  public AbstractQueue values = new PriorityQueue();
              }
              """,
            """
              import java.util.PriorityQueue;
              import java.util.Queue;
              
              class Test {
                  public Queue values = new PriorityQueue();
              }
              """
          )
        );
    }

    @Test
    void concurrentLinkedQueue() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.ConcurrentLinkedQueue;
              
              class Test {
                  public ConcurrentLinkedQueue values = new ConcurrentLinkedQueue();
              }
              """,
            """
              import java.util.Queue;
              import java.util.concurrent.ConcurrentLinkedQueue;
              
              class Test {
                  public Queue values = new ConcurrentLinkedQueue();
              }
              """
          )
        );
    }

    @Test
    void abstractSet() {
        rewriteRun(
          java(
            """
              import java.util.AbstractSet;
              import java.util.HashSet;
              
              class Test {
                  public AbstractSet values = new HashSet();
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set values = new HashSet();
              }
              """
          )
        );
    }

    @Test
    void enumSet() {
        rewriteRun(
          java("public enum A {}"),
          java(
            """
              import java.util.EnumSet;
              
              class Test {
                  public EnumSet values = EnumSet.allOf(A.class);
              }
              """,
            """
              import java.util.EnumSet;
              import java.util.Set;
              
              class Test {
                  public Set values = EnumSet.allOf(A.class);
              }
              """
          )
        );
    }

    @Test
    void hashSet() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  public HashSet values = new HashSet();
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  public Set values = new HashSet();
              }
              """
          )
        );
    }

    @Test
    void linkedHashSet() {
        rewriteRun(
          java(
            """
              import java.util.LinkedHashSet;
              
              class Test {
                  public LinkedHashSet values = new LinkedHashSet();
              }
              """,
            """
              import java.util.LinkedHashSet;
              import java.util.Set;
              
              class Test {
                  public Set values = new LinkedHashSet();
              }
              """
          )
        );
    }

    @Test
    void copyOnWriteArraySet() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.CopyOnWriteArraySet;
              
              class Test {
                  public CopyOnWriteArraySet values = new CopyOnWriteArraySet();
              }
              """,
            """
              import java.util.Set;
              import java.util.concurrent.CopyOnWriteArraySet;
              
              class Test {
                  public Set values = new CopyOnWriteArraySet();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    void privateVariable() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              
              class Test {
                  private ArrayList<Integer> values = new ArrayList<>();
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;
              
              class Test {
                  private List<Integer> values = new ArrayList<>();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    void noModifierOnVariable() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              
              class Test {
                  ArrayList<Integer> values = new ArrayList<>();
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.List;
              
              class Test {
                  List<Integer> values = new ArrayList<>();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    void privateMethod() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  private HashSet<Integer> method() {
                      return new HashSet<>();
                  }
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  private Set<Integer> method() {
                      return new HashSet<>();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1703")
    @Test
    void noModifierOnMethod() {
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              
              class Test {
                  HashSet<Integer> method() {
                      return new HashSet<>();
                  }
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;
              
              class Test {
                  Set<Integer> method() {
                      return new HashSet<>();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1771")
    @Test
    void variableWithVar() {
        var javaRuntimeVersion = System.getProperty("java.runtime.version");
        var javaVendor = System.getProperty("java.vm.vendor");
        if (new JavaVersion(UUID.randomUUID(), javaRuntimeVersion, javaVendor, javaRuntimeVersion, javaRuntimeVersion)
              .getMajorVersion() >= 10) {
            rewriteRun(
              java(
                """
                  import java.util.ArrayList;
                              
                  class Test {
                      public void method() {
                          var list = new ArrayList<>();
                      }
                  }
                  """
              )
            );
        }
    }
}
