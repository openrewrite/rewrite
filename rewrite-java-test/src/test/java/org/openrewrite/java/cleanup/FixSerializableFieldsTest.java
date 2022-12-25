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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

class FixSerializableFieldsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FixSerializableFields(false, null));
    }

    @Language("java")
    String models = """
      import java.io.Serializable;
              
      public class A {
          int value1;
      }
      public class B {
          A aValue;
      }
      public class C implements Serializable {
          int intValue;
          String stringValue;
      }
      """;

    @Test
    void markTransient() {
        rewriteRun(
          java(models),
          java(
            """
              import java.io.Serializable;
              import java.io.DataInputStream;
                            
              class Example implements Serializable {
                  private DataInputStream nonSerializable;
                  C cValue;
                  public void test() {
                  }
              }
              """,
            """
              import java.io.Serializable;
              import java.io.DataInputStream;

              class Example implements Serializable {
                  private transient DataInputStream nonSerializable;
                  C cValue;
                  public void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void markAsTransientArray() {
        rewriteRun(
          java(models),
          java(
            """
                  import java.io.Serializable;
                  import java.io.DataInputStream;

                  class Example implements Serializable {
                      private DataInputStream[] nonSerializable;
                      C cValue;
                      public void test() {
                      }
                  }
              """,
            """
                  import java.io.Serializable;
                  import java.io.DataInputStream;

                  class Example implements Serializable {
                      private transient DataInputStream[] nonSerializable;
                      C cValue;
                      public void test() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void markAsTransientList() {
        rewriteRun(
          java(models),
          java(
            """
                  import java.io.DataInputStream;
                  import java.io.Serializable;
                  import java.util.List;

                  class Example implements Serializable {
                      private List<DataInputStream> aValue;
                      private List<C> cValue;
                      public void test() {
                      }
                  }
              """,
            """
                  import java.io.DataInputStream;
                  import java.io.Serializable;
                  import java.util.List;

                  class Example implements Serializable {
                      private transient List<DataInputStream> aValue;
                      private List<C> cValue;
                      public void test() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void markAsTransientMap() {
        rewriteRun(
          java(models),
          java(
            """
                  import java.io.DataInputStream;
                  import java.io.Serializable;
                  import java.util.Map;

                  class Example implements Serializable {
                      private Map<String, DataInputStream> aMap;
                      private Map<String, C> cMap;
                      public void test() {
                      }
                  }
              """,
            """
                  import java.io.DataInputStream;
                  import java.io.Serializable;
                  import java.util.Map;

                  class Example implements Serializable {
                      private transient Map<String, DataInputStream> aMap;
                      private Map<String, C> cMap;
                      public void test() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void dontMarkStaticFields() {
        rewriteRun(
          java(models),
          java(
            """
                  import java.io.Serializable;

                  class Example implements Serializable {
                      private static A aValue;
                      C cValue;
                      public void test() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void dontModifyClassThatIsNotSerializable() {
        rewriteRun(
          java(models),
          java(
            """
                  class Example {
                      private A aValue;
                      C cValue;
                      public void test() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void makeSerializable() {
        rewriteRun(
          java(models),
          java(
            """
                  import java.io.DataInputStream;
                  import java.io.Serializable;
                  
                  class Example implements Serializable {
                      private DataInputStream nonSerializable;
                      C cValue;
                      public void test() {
                      }
                  }
              """,
            """
                  import java.io.DataInputStream;
                  import java.io.Serializable;

                  class Example implements Serializable {
                      private transient DataInputStream nonSerializable;
                      C cValue;
                      public void test() {
                      }
                  }
              """
          )
        );
    }

    @Test
    void makeSerializableArray() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                  import java.io.DataInputStream;
                  
                  class Example implements Serializable {
                      private A[] nonSerializable;
                      C cValue;
                      public void test() {
                      }
                  }
              """
          ),
          java(
            models,
            """
                  import java.io.Serializable;

                  public class A implements Serializable {
                      int value1;
                  }
                  public class B {
                      A aValue;
                  }
                  public class C implements Serializable {
                      int intValue;
                      String stringValue;
                  }
              """
          )
        );
    }

    @Test
    void makeSerializableList() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                  import java.io.DataInputStream;
                  import java.util.List;
                  
                  class Example implements Serializable {
                      private List<A> nonSerializable;
                      C cValue;
                      public void test() {
                      }
                  }
              """
          ),
          java(
            models,
            """
                  import java.io.Serializable;

                  public class A implements Serializable {
                      int value1;
                  }
                  public class B {
                      A aValue;
                  }
                  public class C implements Serializable {
                      int intValue;
                      String stringValue;
                  }
              """
          )
        );
    }

    @Test
    void makeSerializableMap() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                  import java.io.DataInputStream;
                  import java.util.Map;
                  
                  class Example implements Serializable {
                      private Map<String,A> nonSerializable;
                      C cValue;
                      public void test() {
                      }
                  }
              """
          ),
          java(
            models,
            """
                  import java.io.Serializable;

                  public class A implements Serializable {
                      int value1;
                  }
                  public class B {
                      A aValue;
                  }
                  public class C implements Serializable {
                      int intValue;
                      String stringValue;
                  }
              """
          )
        );
    }

    @Test
    void makeExclusionTransient() {
        rewriteRun(
          spec -> spec.recipe(new FixSerializableFields(false, singletonList("A"))),
            java(models),
            java(
              """
                    import java.io.Serializable;
                    
                    class Example implements Serializable {
                        private A nonSerializable;
                        C cValue;
                        public void test() {
                        }
                    }
                """,
              """
                    import java.io.Serializable;
                    
                    class Example implements Serializable {
                        private transient A nonSerializable;
                        C cValue;
                        public void test() {
                        }
                    }
                """
            )
          );
    }

    @Test
    void doNotChangeSerializableGenerics() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
              import java.util.Map;
                            
              class A<TTTT extends Serializable> implements Serializable {
                  private Map<String, TTTT> items;
                  private TTTT item;
              }
              """
          )
        );
    }
}
