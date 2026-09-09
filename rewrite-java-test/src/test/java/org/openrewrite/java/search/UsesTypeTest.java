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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

class UsesTypeTest implements RewriteTest {

    @DocumentExample
    @Test
    void usesTypeFindsImports() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.Collections", false))),
          java(
            """
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
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2427")
    @Test
    void primitiveTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("double", false))),
          java(
            """
              class Test {
                  double d = 1d;
              }
              """,
            """
              /*~~>*/class Test {
                  double d = 1d;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1169")
    @Test
    void emptyConstructor() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.ArrayList", false))),
          java(
            """
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
              """
          )
        );
    }

    /**
     * Type wildcards are greedy.
     */
    @Test
    void usesTypeWildcardFindsImports() {
        rewriteRun(
          spec ->
            spec.recipe(toRecipe(() -> new UsesType<>("java.util.*", false))),
          java(
            """
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
              """
          )
        );
    }

    /**
     * Type wildcards are greedy.
     */
    @Test
    void usesRecursiveTypeWildcard() {
        rewriteRun(
          spec ->
            spec.recipe(toRecipe(() -> new UsesType<>("java..*", false))),
          java(
            """
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
              """
          )
        );
    }

    @Test
    void usesFullyQualifiedReference() {
        rewriteRun(
          spec ->
            spec.recipe(toRecipe(() -> new UsesType<>("java.util.*", false))),
          java(
            """
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
              """
          )
        );
    }

    @Test
    void usesTypeFindsInheritedTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.Collection", false))),
          java(
            """
              import java.util.List;

              class Test {
              }
              """,
            """
              /*~~>*/import java.util.List;

              class Test {
              }
              """
          )
        );
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void findImplicitTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.List", true))),
          java(
            """
              import java.util.Collections;

              class Test {
                  int zero = Collections.emptyList().size();
              }
              """,
            """
              /*~~>*/import java.util.Collections;

              class Test {
                  int zero = Collections.emptyList().size();
              }
              """
          )
        );
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void findImplicitTypesFalse() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.List", false))),
          java(
            """
              import java.util.Collections;

              class Test {
                  int zero = Collections.emptyList().size();
              }
              """
          )
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void findImplicitTypesParams() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.List", true))),
          java(
            """
              import java.util.Collections;

              class Test {
                  public void foo() {
                      Collections.copy(Collections.emptyList(), Collections.emptyList());
                  }
              }
              """,
            """
              /*~~>*/import java.util.Collections;

              class Test {
                  public void foo() {
                      Collections.copy(Collections.emptyList(), Collections.emptyList());
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void findImplicitTypesParamsFalse() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.List", false))),
          java(
            """
              import java.util.Collections;

              class Test {
                  public void foo() {
                      Collections.copy(Collections.emptyList(), Collections.emptyList());
                  }
              }
              """
          )
        );
    }

    @Test
    void findNestedType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util.Map.Entry", false))),
          java(
            """
              import java.util.Map.Entry;

              class Test {
              }
              """,
            """
              /*~~>*/import java.util.Map.Entry;

              class Test {
              }
              """
          )
        );
    }

    @Test
    void fullWildcardMatchesAnyType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("*..*", false))),
          java(
            """
              import java.util.HashMap;
              class Test {
                  HashMap<String, String> map = new HashMap<>();
              }
              """,
            """
              /*~~>*/import java.util.HashMap;
              class Test {
                  HashMap<String, String> map = new HashMap<>();
              }
              """
          )
        );
    }

    @Test
    void packageRecursiveWildcardHit() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util..*", false))),
          java(
            """
              import java.util.concurrent.ConcurrentHashMap;
              class Test {
                  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
              }
              """,
            """
              /*~~>*/import java.util.concurrent.ConcurrentHashMap;
              class Test {
                  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
              }
              """
          )
        );
    }

    @Test
    void packageRecursiveWildcardMiss() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("javax.swing..*", false))),
          java(
            """
              import java.util.HashMap;
              class Test {
                  HashMap<String, String> map = new HashMap<>();
              }
              """
          )
        );
    }

    @Test
    void subpackageWithNamePatternHit() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.util..*Map*", false))),
          java(
            """
              import java.util.concurrent.ConcurrentHashMap;
              class Test {
                  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
              }
              """,
            """
              /*~~>*/import java.util.concurrent.ConcurrentHashMap;
              class Test {
                  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
              }
              """
          )
        );
    }

    @Test
    void subpackageWithNamePatternMiss() {
        // The package prefix doesn't match — `javax.swing` doesn't appear in the closure of a
        // file that only references `java.util.HashMap`.
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("javax.swing..*Frame*", false))),
          java(
            """
              import java.util.HashMap;
              class Test {
                  HashMap<String, String> map = new HashMap<>();
              }
              """
          )
        );
    }

    @Test
    void middleWildcardHit() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.*.ArrayList", false))),
          java(
            """
              import java.util.ArrayList;
              class Test {
                  ArrayList<String> list = new ArrayList<>();
              }
              """,
            """
              /*~~>*/import java.util.ArrayList;
              class Test {
                  ArrayList<String> list = new ArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void middleWildcardMiss() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java.*.ArrayList", false))),
          java(
            """
              import java.util.concurrent.ConcurrentHashMap;
              class Test {
                  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
              }
              """
          )
        );
    }

    @Test
    void doubleDotIsLiteralWithoutWildcard() {
        // No '*' in the input means UsesType treats it as an exact FQN. Real Java FQNs never have
        // consecutive dots, so this should miss every well-formed compilation unit.
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("java..util.ArrayList", false))),
          java(
            """
              import java.util.ArrayList;
              class Test {
                  ArrayList<String> list = new ArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void nestedTypeInDottedForm() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("a.b.Outer.Inner", false))),
          java(
            """
              package a.b;
              public class Outer {
                  public static class Inner {}
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import a.b.Outer;
              class Test {
                  Outer.Inner i;
              }
              """,
            """
              /*~~>*/import a.b.Outer;
              class Test {
                  Outer.Inner i;
              }
              """
          )
        );
    }

    @Test
    void nestedTypeInBinaryForm() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("a.b.Outer$Inner", false))),
          java(
            """
              package a.b;
              public class Outer {
                  public static class Inner {}
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import a.b.Outer;
              class Test {
                  Outer.Inner i;
              }
              """,
            """
              /*~~>*/import a.b.Outer;
              class Test {
                  Outer.Inner i;
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.b.Outer.Mid.Inner", "a.b.Outer$Mid$Inner", "a.b.Outer.Mid$Inner"})
    void doublyNestedType(String type) {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>(type, false))),
          java(
            """
              package a.b;
              public class Outer {
                  public static class Mid {
                      public static class Inner {}
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import a.b.Outer;
              class Test {
                  Outer.Mid.Inner i;
              }
              """,
            """
              /*~~>*/import a.b.Outer;
              class Test {
                  Outer.Mid.Inner i;
              }
              """
          )
        );
    }

    @Test
    void topLevelTypeMiss() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("a.b.Other", false))),
          java(
            """
              package a.b;
              public class Thing {}
              """,
            SourceSpec::skip
          ),
          java(
            """
              import a.b.Thing;
              class Test {
                  Thing t;
              }
              """
          )
        );
    }

    @Test
    void packageSegmentStartingUppercase() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>("a.B.Thing", false))),
          java(
            """
              package a.B;
              public class Thing {}
              """,
            SourceSpec::skip
          ),
          java(
            """
              import a.B.Thing;
              class Test {
                  Thing t;
              }
              """,
            """
              /*~~>*/import a.B.Thing;
              class Test {
                  Thing t;
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.b.Outer.Inner", "a.b.Outer$Inner"})
    void nestedTypeReference(String type) {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesType<>(type, false))),
          xml(
            """
              <beans xsi:schemaLocation="www.springframework.org/schema/beans">
                  <bean id="abc" class="a.b.Outer$Inner"/>
              </beans>
              """,
            """
              <!--~~>--><beans xsi:schemaLocation="www.springframework.org/schema/beans">
                  <bean id="abc" class="a.b.Outer$Inner"/>
              </beans>
              """
          )
        );
    }
}
