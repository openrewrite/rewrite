/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

class ModuleUsesTypeTest implements RewriteTest {

    @DocumentExample
    @Test
    void marksAllFilesInModuleUsingType() {
        rewriteRun(
          spec -> spec.recipe(new ModuleUsesType("java.util.List", null)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  import java.util.List;
                  class A {
                      List<String> items;
                  }
                  """,
                """
                  /*~~(Module uses type: java.util.List)~~>*/import java.util.List;
                  class A {
                      List<String> items;
                  }
                  """
              ),
              java(
                """
                  class B {}
                  """,
                """
                  /*~~(Module uses type: java.util.List)~~>*/class B {}
                  """
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  class C {}
                  """
              )
            )
          )
        );
    }

    @Test
    void noMatchingType() {
        rewriteRun(
          spec -> spec.recipe(new ModuleUsesType("com.nonexistent.Type", null)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  class A {}
                  """
              )
            )
          )
        );
    }

    @Test
    void wildcardPattern() {
        rewriteRun(
          spec -> spec.recipe(new ModuleUsesType("java.util.*", null)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  import java.util.ArrayList;
                  class A {
                      ArrayList<String> items = new ArrayList<>();
                  }
                  """,
                """
                  /*~~(Module uses type: java.util.*)~~>*/import java.util.ArrayList;
                  class A {
                      ArrayList<String> items = new ArrayList<>();
                  }
                  """
              ),
              java(
                """
                  class B {}
                  """,
                """
                  /*~~(Module uses type: java.util.*)~~>*/class B {}
                  """
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  class C {}
                  """
              )
            )
          )
        );
    }

    @Test
    void recursiveWildcardPattern() {
        rewriteRun(
          spec -> spec.recipe(new ModuleUsesType("java.util..*", null)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  import java.util.concurrent.ConcurrentHashMap;
                  class A {
                      ConcurrentHashMap<String, String> map;
                  }
                  """,
                """
                  /*~~(Module uses type: java.util..*)~~>*/import java.util.concurrent.ConcurrentHashMap;
                  class A {
                      ConcurrentHashMap<String, String> map;
                  }
                  """
              ),
              java(
                """
                  class B {}
                  """,
                """
                  /*~~(Module uses type: java.util..*)~~>*/class B {}
                  """
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  class C {}
                  """
              )
            )
          )
        );
    }

    @Test
    void includeImplicitUsages() {
        rewriteRun(
          spec -> spec.recipe(new ModuleUsesType("java.util.List", true)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  import java.util.Collections;
                  class A {
                      int zero = Collections.emptyList().size();
                  }
                  """,
                """
                  /*~~(Module uses type: java.util.List)~~>*/import java.util.Collections;
                  class A {
                      int zero = Collections.emptyList().size();
                  }
                  """
              ),
              java(
                """
                  class B {}
                  """,
                """
                  /*~~(Module uses type: java.util.List)~~>*/class B {}
                  """
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  class C {}
                  """
              )
            )
          )
        );
    }

    @Test
    void implicitUsagesFalseDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ModuleUsesType("java.util.List", false)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  import java.util.Collections;
                  class A {
                      int zero = Collections.emptyList().size();
                  }
                  """
              )
            )
          )
        );
    }

    @Test
    void multipleModulesUsingType() {
        rewriteRun(
          spec -> spec.recipe(new ModuleUsesType("java.util.List", null)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  import java.util.List;
                  class A {
                      List<String> items;
                  }
                  """,
                """
                  /*~~(Module uses type: java.util.List)~~>*/import java.util.List;
                  class A {
                      List<String> items;
                  }
                  """
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  import java.util.List;
                  class B {
                      List<Integer> numbers;
                  }
                  """,
                """
                  /*~~(Module uses type: java.util.List)~~>*/import java.util.List;
                  class B {
                      List<Integer> numbers;
                  }
                  """
              )
            )
          ),
          mavenProject("module-c",
            srcMainJava(
              java(
                """
                  class C {}
                  """
              )
            )
          )
        );
    }
}
