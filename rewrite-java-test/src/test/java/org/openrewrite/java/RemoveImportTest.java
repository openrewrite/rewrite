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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class RemoveImportTest implements RewriteTest {
    private static Recipe removeImport(String type) {
        return removeImport(type, false);
    }

    private static Recipe removeImport(String type, boolean force) {
        return toRecipe(() -> new RemoveImport<>(type, force));
    }

    @DocumentExample
    @Test
    void removeNamedImport() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              import java.util.List;
              class A {}
              """,
            "class A {}"
          )
        );
    }

    @Test
    void leaveImportIfRemovedTypeIsStillReferredTo() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              import java.util.List;
              class A {
                 List<Integer> list;
              }
              """
          )
        );
    }

    @Test
    void leaveWildcardImportIfRemovedTypeIsStillReferredTo() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.*")),
          java(
            """
              import java.util.*;
              class A {
                 List<Integer> list;
              }
              """
          )
        );
    }

    @Test
    void removeStarImportIfNoTypesReferredTo() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              import java.util.*;
              class A {}
              """,
            "class A {}"
          )
        );
    }

    @Test
    void replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              import java.util.*;
                            
              class A {
                 Collection<Integer> c;
              }
              """,
            """
              import java.util.Collection;
                            
              class A {
                 Collection<Integer> c;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/919")
    @Test
    void leaveStarImportInPlaceIfFiveOrMoreTypesStillReferredTo() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              import java.util.*;
              class A {
                 Collection<Integer> c;
                 Set<Integer> s = new HashSet<>();
                 Map<Integer, Integer> m = new HashMap<>();
              }
              """
          )
        );
    }

    @Test
    void removeStarStaticImport() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections")),
          java(
            """
              import static java.util.Collections.*;
              class A {}
              """,
            "class A {}"
          )
        );
    }

    @Test
    void removeStarStaticImportWhenRemovingSpecificMethod() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections.emptyList")),
          java(
            """
              import static java.util.Collections.*;
              class A {
                  Object o = emptySet();
              }
              """,
            """
              import static java.util.Collections.emptySet;
              class A {
                  Object o = emptySet();
              }
              """
          )
        );
    }

    @Test
    void removeStarImportEvenIfReferredTo() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List", true)),
          java(
            """
              import java.util.List;
              class A {
                  List<String> l;
              }
              """,
            """
              class A {
                  List<String> l;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/918")
    @Test
    void leaveStarStaticImportIfReferenceStillExists() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections")),
          java(
            """
              import static java.util.Collections.*;
              class A {
                 Object o = emptyList();
              }
              """,
            """
              import static java.util.Collections.emptyList;
              class A {
                 Object o = emptyList();
              }
              """
          )
        );
    }

    @Test
    void removeStaticImport() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.time.DayOfWeek.MONDAY")),
          java(
            """
              import java.time.DayOfWeek;
                            
              import static java.time.DayOfWeek.MONDAY;
              import static java.time.DayOfWeek.TUESDAY;
                            
              class WorkWeek {
                  DayOfWeek shortWeekStarts(){
                      return TUESDAY;
                  }
              }
              """,
            """
              import java.time.DayOfWeek;
                            
              import static java.time.DayOfWeek.TUESDAY;
                            
              class WorkWeek {
                  DayOfWeek shortWeekStarts(){
                      return TUESDAY;
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveNamedStaticImportIfReferenceStillExists() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections")),
          java(
            """
              import static java.util.Collections.emptyList;
              import static java.util.Collections.emptySet;
                            
              class A {
                 Object o = emptyList();
              }
              """,
            """
              import static java.util.Collections.emptyList;
                            
              class A {
                 Object o = emptyList();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    void preservesWhitespaceAfterPackageDeclaration() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              package com.example.foo;
                            
              import java.util.List;
              import java.util.ArrayList;
                            
              public class A {
                  ArrayList<String> foo = new ArrayList<>();
              }
              """,
            """
              package com.example.foo;
                            
              import java.util.ArrayList;
                            
              public class A {
                  ArrayList<String> foo = new ArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void preservesWhitespaceAfterRemovedImport() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              package com.example.foo;
                            
              import java.util.Collection;
              import java.util.List;
                            
              import java.util.ArrayList;
                            
              public class A {
              }
              """,
            """
              package com.example.foo;
                            
              import java.util.Collection;
                            
              import java.util.ArrayList;
                            
              public class A {
              }
              """
          )
        );
    }

    @Test
    void preservesWhitespaceAfterRemovedImportForWindowsWhitespace() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
              "package com.example.foo;\r\n" +
              "\r\n" +
              "import java.util.Collection;\r\n" +
              "import java.util.List;\r\n" +
              "\r\n" +
              "import java.util.ArrayList;\r\n" +
              "\r\n" +
              "public class A {\r\n" +
              "}\r\n",
              "package com.example.foo;\r\n" +
              "\r\n" +
              "import java.util.Collection;\r\n" +
              "\r\n" +
              "import java.util.ArrayList;\r\n" +
              "\r\n" +
              "public class A {\r\n" +
              "}\r\n"
          )
        );
    }

    @Test
    void preservesWhitespaceAfterRemovedImportForMixedWhitespaceBefore() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
              "package com.example.foo;\n" +
              "\n" +
              "import java.util.Collection;\r\n" +
              "import java.util.List;\n" +
              "import java.util.ArrayList;\n" +
              "\n" +
              "public class A {\n" +
              "}\n",
              "package com.example.foo;\n" +
              "\n" +
              "import java.util.Collection;\n" +
              "import java.util.ArrayList;\n" +
              "\n" +
              "public class A {\n" +
              "}\n"
          )
        );
    }

    @Test
    void preservesWhitespaceAfterRemovedImportForMixedWhitespaceAfter() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
              "package com.example.foo;\n" +
              "\n" +
              "import java.util.Collection;\n" +
              "\n" +
              "import java.util.List;\r\n" +
              "import java.util.ArrayList;\n" +
              "\n" +
              "public class A {\n" +
              "}\n",
              "package com.example.foo;\n" +
              "\n" +
              "import java.util.Collection;\n" +
              "\n" +
              "import java.util.ArrayList;\n" +
              "\n" +
              "public class A {\n" +
              "}\n"
          )
        );
    }

    @Test
    void doNotLeaveLeaveInitialBlankLine() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collection")),
          java(
            """
              import static java.util.Collection.*;
              import static java.util.List.*;
                            
              public class A {
              }
              """,
            """
              import static java.util.List.*;
                            
              public class A {
              }
              """
          )
        );
    }

    @Test
    void preservesCommentAfterRemovedImport() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              package com.example.foo;
                            
              import java.util.List;
              // import java.util.UUID
              import java.util.ArrayList;
                            
              public class A {
              }
              """,
            """
              package com.example.foo;
                            
              // import java.util.UUID
              import java.util.ArrayList;
                            
              public class A {
              }
              """
          )
        );
    }

    @Test
    void preservesWhitespaceAfterRemovedStaticImport() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections.singletonList")),
          java(
            """
              package com.example.foo;

              import static java.util.Collections.emptySet;
              import static java.util.Collections.singletonList;
                            
              import java.util.UUID;
                            
              public class A {
              }
              """,
            """
              package com.example.foo;
                            
              import static java.util.Collections.emptySet;

              import java.util.UUID;
                            
              public class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    void preservesWhitespaceAfterPackageDeclarationNoImportsRemain() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              package com.example.foo;
                            
              import java.util.List;
                            
              public class A {
              }
              """,
            """
              package com.example.foo;
                            
              public class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    void preservesWhitespaceBetweenGroupsOfImports() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              package com.yourorg.b;
              public class B {}
              """
          ),
          java(
            """
              package com.example.foo;
                            
              import com.yourorg.b.B;
                            
              import java.util.List;
              import java.util.ArrayList;
                            
              public class A {
                  ArrayList<B> foo = new ArrayList<>();
              }
              """,
            """
              package com.example.foo;
                            
              import com.yourorg.b.B;
                            
              import java.util.ArrayList;
                            
              public class A {
                  ArrayList<B> foo = new ArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void doesNotAffectClassBodyFormatting() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.List")),
          java(
            """
              package com.example.foo;
                            
              import java.util.List;
              import java.util.ArrayList;
                            
              public class A {
              // Intentionally misaligned to ensure AutoFormat has not been applied to the class body
              ArrayList<String> foo = new ArrayList<>();
              }
              """,
            """
              package com.example.foo;
                            
              import java.util.ArrayList;
                            
              public class A {
              // Intentionally misaligned to ensure AutoFormat has not been applied to the class body
              ArrayList<String> foo = new ArrayList<>();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/784")
    @Test
    void removeFromWildcardAndDuplicateImport() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collection")),
          java(
            """
              package a;

              import java.util.*;
              import java.util.List;

              public class A {
                  Set<Integer> s;
                  List<Integer> l;
              }
              """,
            """
              package a;

              import java.util.Set;
              import java.util.List;

              public class A {
                  Set<Integer> s;
                  List<Integer> l;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/781")
    @Test
    void generateNewUUIDPerUnfoldedImport() {
        rewriteRun(
          spec -> spec.recipes(
            removeImport("java.util.Collection"),
            new ChangeType("java.util.List", "java.util.Collection", null)
          ),
          java(
            """
              package a;

              import java.util.*;
              import java.util.List;

              public class A {
                  Set<Integer> s;
                  List<Integer> l;
              }
              """,
            """
              package a;

              import java.util.Collection;
              import java.util.Set;

              public class A {
                  Set<Integer> s;
                  Collection<Integer> l;
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getImports().get(0).getId()).isNotEqualTo(cu.getImports().get(1).getId())))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/918")
    @Test
    void leaveAloneIfThreeOrMoreStaticsAreInUse() {
        rewriteRun(
          spec -> spec.recipe(removeImport("org.test.Example.VALUE_1")),
          java(
            """
              package org.test;
              public class Example {
                  public static final int VALUE_1 = 1;
                  public static final int VALUE_2 = 2;
                  public static final int VALUE_3 = 3;
                  public static int method1() { return 1; }
              }
              """
          ),
          java(
            """
              package org.test.a;
                            
              import static org.test.Example.*;
                            
              public class Test {
                  public void method() {
                      int value2 = VALUE_2;
                      int value3 = VALUE_3;
                      int methodValue = method1();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/918")
    @Test
    void unfoldStaticImportIfTwoOrLessAreUsed() {
        rewriteRun(
          spec -> spec.recipe(removeImport("org.test.Example.VALUE_1")),
          java(
            """
              package org.test;
              public class Example {
                  public static final int VALUE_1 = 1;
                  public static final int VALUE_2 = 2;
                  public static final int VALUE_3 = 3;
                  public static int method1() { return 1; }
              }
              """
          ),
          java(
            """
              package org.test.a;
                            
              import static org.test.Example.*;
                            
              public class Test {
                  public void method() {
                      int value2 = VALUE_2;
                      int methodValue = method1();
                  }
              }
              """,
            """
              package org.test.a;
                            
              import static org.test.Example.method1;
              import static org.test.Example.VALUE_2;
                            
              public class Test {
                  public void method() {
                      int value2 = VALUE_2;
                      int methodValue = method1();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotUnfoldPackage() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Map")).parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                randomId(), "test", "test", "test", emptySet(), singletonList(
                ImportLayoutStyle.builder()
                  .packageToFold("java.util.*")
                  .importAllOthers()
                  .importStaticAllOthers()
                  .build()
              )
              )
            )
          )),
          java(
            """
              import java.util.*;
                            
              class Test {
                  List<String> l;
              }
              """
          )
        );
    }

    @Test
    void doNotUnfoldSubPackage() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.concurrent.ConcurrentLinkedQueue")).parser(
            JavaParser.fromJavaVersion().styles(
              singletonList(
                new NamedStyles(
                  randomId(), "test", "test", "test", emptySet(), singletonList(
                  ImportLayoutStyle.builder()
                    .packageToFold("java.util.*")
                    .importAllOthers()
                    .importStaticAllOthers()
                    .build()
                )
                )
              )
            )
          ),
          java(
            """
              import java.util.*;
              import java.util.concurrent.*;
                            
              class Test {
                  Map<Integer, Integer> m = new ConcurrentHashMap<>();
              }
              """
          )
        );
    }

    @Test
    void unfoldSubpackage() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.concurrent.ConcurrentLinkedQueue")).parser(
            JavaParser.fromJavaVersion().styles(
              singletonList(
                new NamedStyles(
                  randomId(), "test", "test", "test", emptySet(), singletonList(
                  ImportLayoutStyle.builder()
                    .packageToFold("java.util.*", false)
                    .importAllOthers()
                    .importStaticAllOthers()
                    .build()
                )
                )
              )
            )
          ),
          java(
            """
              import java.util.*;
              import java.util.concurrent.*;
                            
              class Test {
                  Map<Integer, Integer> m = new ConcurrentHashMap<>();
              }
              """,
            """
              import java.util.*;
              import java.util.concurrent.ConcurrentHashMap;
                            
              class Test {
                  Map<Integer, Integer> m = new ConcurrentHashMap<>();
              }
              """
          )
        );
    }

    @Test
    void doNotUnfoldStaticPackage() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections.emptyMap")).parser(
            JavaParser.fromJavaVersion().styles(
              singletonList(
                new NamedStyles(
                  randomId(), "test", "test", "test", emptySet(), singletonList(
                  ImportLayoutStyle.builder()
                    .staticPackageToFold("java.util.Collections.*")
                    .importAllOthers()
                    .importStaticAllOthers()
                    .build()
                )
                )
              )
            )
          ),
          java(
            """
              import java.util.*;
              import static java.util.Collections.*;
                            
              class Test {
                  List<String> l = emptyList();
              }
              """
          )
        );
    }

    @Test
    void doNotUnfoldStaticSubPackage() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections.emptyMap")).parser(
            JavaParser.fromJavaVersion().styles(
              singletonList(
                new NamedStyles(
                  randomId(), "test", "test", "test", emptySet(), singletonList(
                  ImportLayoutStyle.builder()
                    .staticPackageToFold("java.util.*")
                    .importAllOthers()
                    .importStaticAllOthers()
                    .build()
                )
                )
              )
            )
          ),
          java(
            """
              import java.util.List;
              import static java.util.Collections.*;
                            
              class Test {
                  List<Integer> l = emptyList();
              }
              """
          )
        );
    }

    @Test
    void unfoldStaticSubpackage() {
        rewriteRun(
          spec -> spec.recipe(removeImport("java.util.Collections.emptyMap")).parser(
            JavaParser.fromJavaVersion().styles(
              singletonList(
                new NamedStyles(
                  randomId(), "test", "test", "test", emptySet(), singletonList(
                  ImportLayoutStyle.builder()
                    .packageToFold("java.util.*", false)
                    .importAllOthers()
                    .importStaticAllOthers()
                    .build()
                )
                )
              )
            )
          ),
          java(
            """
              import java.util.List;
              import static java.util.Collections.*;
                            
              class Test {
                  List<Integer> l = emptyList();
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
}
