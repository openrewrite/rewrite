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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("unchecked")
class NoEmptyCollectionWithRawTypeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoEmptyCollectionWithRawType());
    }

    @Test
    void emptyListFullyQualified() {
        rewriteRun(
          java(
            """
              import java.util.List;
                            
                            class Test {
                  List<Integer> l = java.util.Collections.EMPTY_LIST;
              }
              """,
            """
              import java.util.List;
                            
                            class Test {
                  List<Integer> l = java.util.Collections.emptyList();
              }
              """
          )
        );
    }

    @Test
    void emptyListStaticImport() {
        rewriteRun(
          java(
            """
              import java.util.List;
              
              import static java.util.Collections.EMPTY_LIST;
              
                            class Test {
                  List<Integer> l = EMPTY_LIST;
              }
              """,
            """
              import java.util.List;
              
              import static java.util.Collections.emptyList;
              
                            class Test {
                  List<Integer> l = emptyList();
              }
              """
          )
        );
    }

    @Test
    void emptyListFieldAccess() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              import java.util.List;
              
                            class Test {
                  List<Integer> l = Collections.EMPTY_LIST;
              }
              """,
            """
              import java.util.Collections;
              import java.util.List;
              
                            class Test {
                  List<Integer> l = Collections.emptyList();
              }
              """
          )
        );
    }

    @Test
    void emptyMapFullyQualified() {
        rewriteRun(
          java(
            """
              import java.util.Map;
              
                            class Test {
                  Map<Integer, Integer> m = java.util.Collections.EMPTY_MAP;
              }
              """,
            """
              import java.util.Map;
              
                            class Test {
                  Map<Integer, Integer> m = java.util.Collections.emptyMap();
              }
              """
          )
        );
    }

    @Test
    void emptyMapStaticImport() {
        rewriteRun(
          java(
            """
              import java.util.Map;
              
              import static java.util.Collections.EMPTY_MAP;
              
                            class Test {
                  Map<Integer, Integer> l = EMPTY_MAP;
              }
              """,
            """
              import java.util.Map;
              
              import static java.util.Collections.emptyMap;
              
                            class Test {
                  Map<Integer, Integer> l = emptyMap();
              }
              """
          )
        );
    }

    @Test
    void emptyMapFieldAccess() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              import java.util.Map;
              
                            class Test {
                  Map<Integer, Integer> m = Collections.EMPTY_MAP;
              }
              """,
            """
              import java.util.Collections;
              import java.util.Map;
              
                            class Test {
                  Map<Integer, Integer> m = Collections.emptyMap();
              }
              """
          )
        );
    }

    @Test
    void emptySetFullyQualified() {
        rewriteRun(
          java(
            """
              import java.util.Set;
              
                            class Test {
                  Set<Integer> m = java.util.Collections.EMPTY_SET;
              }
              """,
            """
              import java.util.Set;
              
                            class Test {
                  Set<Integer> m = java.util.Collections.emptySet();
              }
              """
          )
        );
    }

    @Test
    void emptySetStaticImport() {
        rewriteRun(
          java(
            """
              import java.util.Set;
              
              import static java.util.Collections.EMPTY_SET;
              
                            class Test {
                  Set<Integer> l = EMPTY_SET;
              }
              """,
            """
              import java.util.Set;
              
              import static java.util.Collections.emptySet;
              
                            class Test {
                  Set<Integer> l = emptySet();
              }
              """
          )
        );
    }

    @Test
    void emptySetFieldAccess() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              import java.util.Set;
              
                            class Test {
                  Set<Integer> s = Collections.EMPTY_SET;
              }
              """,
            """
              import java.util.Collections;
              import java.util.Set;
              
                            class Test {
                  Set<Integer> s = Collections.emptySet();
              }
              """
          )
        );
    }
}
