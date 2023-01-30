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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"Convert2Diamond", "ResultOfMethodCallIgnored", "StringOperationCanBeSimplified", "rawtypes"})
class NoDoubleBraceInitializationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoDoubleBraceInitialization());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2674")
    @Test
    void possibleMistakenlyMissedAddingToCollection() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class A {
                  void example() {
                      OTList otList = new OTList() {{ new OTElement();}};
                  }
              }
              """,
            """
              import java.util.List;
              class A {
                  void example() {
                      OTList otList = new OTList() {{ /*~~(Did you mean to invoke add() method to the collection?)~~>*/new OTElement();}};
                  }
              }
              """
          ),
          java(
            """
              class OTElement {
              }
              """
          ),
          java(
            """
              import java.util.ArrayList;
              class OTList extends ArrayList {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2674")
    @Test
    void possibleMistakenlyMissedAddingToCollectionWithDifferentMethodName() {
        rewriteRun(
          java(
            """
                import java.util.List;
                import java.util.*;
                
                class A {
                    private final Map<String, String> map = new HashMap<String, String>() {{
                        new AbstractMap.SimpleEntry<>("key", "value");
                    }};
                    private final List<String> list = new ArrayList<String>() {{
                        new String("foo");
                        new String("bar");
                    }};
                    private final Set<String> set = new HashSet<String>() {{
                        new String("foo");
                        new String("bar");
                    }};
                }
              """,
            """
                import java.util.List;
                import java.util.*;
                
                class A {
                    private final Map<String, String> map = new HashMap<String, String>() {{
                        /*~~(Did you mean to invoke put() method to the collection?)~~>*/new AbstractMap.SimpleEntry<>("key", "value");
                    }};
                    private final List<String> list = new ArrayList<String>() {{
                        /*~~(Did you mean to invoke add() method to the collection?)~~>*/new String("foo");
                        /*~~(Did you mean to invoke add() method to the collection?)~~>*/new String("bar");
                    }};
                    private final Set<String> set = new HashSet<String>() {{
                        /*~~(Did you mean to invoke add() method to the collection?)~~>*/new String("foo");
                        /*~~(Did you mean to invoke add() method to the collection?)~~>*/new String("bar");
                    }};
                }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2674")
    @Test
    void noCollectionInitializedInDoubleBraceIgnored() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;
              class A<T> {
                  void example() {
                      Map<String, String> map = new HashMap<String, String>() {
                          {
                            func1();
                          }
                       };
                  }
                  void func1() {
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleBranchInitializationForArgIgnored() {
        rewriteRun(
          java(
            """
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
        );
    }

    @Test
    void doubleBranchInitializationForNewClassArgIgnored() {
        rewriteRun(
          java(
            """
              package abc;
              import java.util.List;

              public class Thing {
                  private final List<String> stuff;
                  public Thing(List<String> stuff) {
                      this.stuff = stuff;
                  }
              }
              """
          ),
          java(
            """
              package abc;
              import java.util.ArrayList;
              import java.util.List;

              class A {
                  Thing t = new Thing(new ArrayList<String>(){{add("abc"); add("def");}});
              }
              """
          )
        );
    }

    @Test
    void doubleBraceInitWithinConstructorArgIgnored() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.Collections;
              import java.util.ArrayList;
              class A {
                  private final List<String> expectedCommand =
                    Collections.unmodifiableList(
                        new ArrayList<String>() {
                          {
                            add("a");
                            add("b");
                          }
                        });
              }
              """
          )
        );
    }

    @SuppressWarnings("WrapperTypeMayBePrimitive")
    @Test
    void addStatementInForLoop() {
        rewriteRun(
          java(
            """
              import java.util.Set;
              import java.util.LinkedHashSet;
              class A {
                  void a() {
                      Integer CNT = 10;
                      final Set<Integer> keys = new LinkedHashSet<>(){{
                          for (int i = 0; i < CNT; i++) {
                              add(i);
                          }
                      }};
                  }
              }
              """,
            """
              import java.util.Set;
              import java.util.LinkedHashSet;
              class A {
                  void a() {
                      Integer CNT = 10;
                      final Set<Integer> keys = new LinkedHashSet<>();
                      for (int i = 0; i < CNT; i++) {
                          keys.add(i);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleBraceInitializationForFieldVar() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.HashMap;
              import java.util.HashSet;
              import java.util.List;
              import java.util.Map;
              import java.util.Set;
                            
              class A {
                  private static final Map<String, String> map = new HashMap<>() {{put("a", "a");}};
                  private final List<String> lst = new ArrayList<>() {{add("x");add("y");}};
                  private final Set<String> mySet = new HashSet<>(){{add("q");}};
              }
              """,
            """
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
        );
    }

    @Test
    void memberVar() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;
              class A {
                  void example() {
                      Map<String, String> aMap = new HashMap<>();
                      aMap.put("c", "c");
                      String s = "x";
                      Map<String, String> bMap = new HashMap<>(){{
                          s.concat("z");
                          put("a", "A");
                          put("b", "B");
                      }};
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;
              class A {
                  void example() {
                      Map<String, String> aMap = new HashMap<>();
                      aMap.put("c", "c");
                      String s = "x";
                      Map<String, String> bMap = new HashMap<>();
                      s.concat("z");
                      bMap.put("a", "A");
                      bMap.put("b", "B");
                  }
              }
              """
          )
        );
    }

    @Test
    void anonymousSubClassMethodInvoked() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class A {
                  void example() {
                      Map<String, String> bMap = new HashMap<String, String>() {
                          {
                              subClassMethod();
                              put("a", "A");
                              put("b", "B");
                          }
                          void subClassMethod() {
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void selectIsThis() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;
              class A {
                  void example() {
                      Map<String, String> bMap = new HashMap<>(){{
                          this.put("a", "A");
                          this.put("b", "B");
                      }};
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;
              class A {
                  void example() {
                      Map<String, String> bMap = new HashMap<>();
                      bMap.put("a", "A");
                      bMap.put("b", "B");
                  }
              }
              """
          )
        );
    }
}
