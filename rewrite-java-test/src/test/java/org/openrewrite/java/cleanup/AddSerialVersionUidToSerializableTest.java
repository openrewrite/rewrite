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

@SuppressWarnings("MissingSerialAnnotation")
class AddSerialVersionUidToSerializableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddSerialVersionUidToSerializable());
    }

    @Test
    void doNothingNotSerializable() {
        rewriteRun(
          java(
            """
              public class Example {
                  private String fred;
                  private int numberOfFreds;
              }
              """
          )
        );
    }

    @Test
    void addSerialVersionUID() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
                          
              public class Example implements Serializable {
                  private String fred;
                  private int numberOfFreds;
              }
              """,
            """
              import java.io.Serializable;
                          
              public class Example implements Serializable {
                  private static final long serialVersionUID = 1;
                  private String fred;
                  private int numberOfFreds;
              }
              """
          )
        );
    }

    @Test
    void fixSerialVersionUIDModifiers() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
                          
              public class Example implements Serializable {
                  private final long serialVersionUID = 1;
                  private String fred;
                  private int numberOfFreds;
              }
              """,
            """
              import java.io.Serializable;
                          
              public class Example implements Serializable {
                  private static final long serialVersionUID = 1;
                  private String fred;
                  private int numberOfFreds;
              }
              """
          )
        );
    }

    @Test
    void fixSerialVersionUIDNoModifiers() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
                          
              public class Example implements Serializable {
                  long serialVersionUID = 1;
                  private String fred;
                  private int numberOfFreds;
              }
              """,
            """
              import java.io.Serializable;
                          
              public class Example implements Serializable {
                  private static final long serialVersionUID = 1;
                  private String fred;
                  private int numberOfFreds;
              }
              """
          )
        );
    }

    @Test
    void fixSerialVersionUIDNoModifiersWrongType() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;

                  public class Example implements Serializable {
                      Long serialVersionUID = 1L;
                      private String fred;
                      private int numberOfFreds;
                  }
              """,
            """
                  import java.io.Serializable;

                  public class Example implements Serializable {
                      private static final long serialVersionUID = 1L;
                      private String fred;
                      private int numberOfFreds;
                  }
              """
          )
        );
    }

    @Test
    void uidAlreadyPresent() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                              
                  public class Example implements Serializable {
                      private static final long serialVersionUID = 1;
                      private String fred;
                      private int numberOfFreds;
                  }
              """
          )
        );
    }

    @Test
    void methodDeclarationsAreNotVisited() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                              
                  public class Example implements Serializable {
                      private String fred;
                      private int numberOfFreds;
                      void doSomething() {
                          int serialVersionUID = 1;
                      }
                  }
              """,
            """
                  import java.io.Serializable;
                              
                  public class Example implements Serializable {
                      private static final long serialVersionUID = 1;
                      private String fred;
                      private int numberOfFreds;
                      void doSomething() {
                          int serialVersionUID = 1;
                      }
                  }
              """
          )
        );
    }

    @Test
    void doNotAlterAnInterface() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                              
                  public interface Example extends Serializable {
                  }
              """
          )
        );
    }

    @Test
    void doNotAlterAnException() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                              
                  public class MyException extends Exception implements Serializable {
                  }
              """
          )
        );
    }

    @Test
    void doNotAlterARuntimeException() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                              
                  public class MyException extends RuntimeException implements Serializable {
                  }
              """
          )
        );
    }

    @Test
    void serializableInnerClass() {
        rewriteRun(
          java(
            """
                  import java.io.Serializable;
                  public class Outer {
                      public static class Inner implements Serializable {
                      
                      }
                  }
              """,
            """
                  import java.io.Serializable;
                  public class Outer {
                      public static class Inner implements Serializable {
                          private static final long serialVersionUID = 1;
                      
                      }
                  }
              """
          )
        );
    }
}
