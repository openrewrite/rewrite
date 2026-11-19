/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveCatchOfExceptionNoLongerThrownTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveCatchOfExceptionNoLongerThrown("lib.Json *(..)", "java.text.ParseException"))
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package lib;
              public class Json {
                  public void exists(String content) throws java.text.ParseException {
                  }
                  public void doesNotExist(String content) throws java.text.ParseException {
                  }
              }
              """,
            """
              package lib;
              public class Schema {
                  public void populate(java.sql.Connection connection) throws java.sql.SQLException {
                  }
              }
              """
          ));
    }

    @DocumentExample
    @Test
    void removeCatchAroundCalls() {
        rewriteRun(
          //language=java
          java(
            """
              import lib.Json;

              import java.text.ParseException;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      try {
                          json.exists(content);
                      } catch (ParseException e) {
                          throw new AssertionError("Not JSON: " + content);
                      }
                  }
              }
              """,
            """
              import lib.Json;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      json.exists(content);
                  }
              }
              """
          )
        );
    }

    @Test
    void otherCatchesAndMultiCatchAlternativesStay() {
        rewriteRun(
          //language=java
          java(
            """
              import lib.Json;

              import java.text.ParseException;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      try {
                          json.exists(content);
                      } catch (ParseException e) {
                          throw new AssertionError("Not JSON: " + content);
                      } catch (IllegalArgumentException e) {
                          throw new AssertionError("Bad path", e);
                      }
                      try {
                          json.doesNotExist(content);
                      } catch (ParseException | IllegalArgumentException e) {
                          throw new AssertionError(e);
                      }
                      try {
                          json.exists(content);
                      } catch (IllegalStateException | ParseException | IllegalArgumentException e) {
                          throw new AssertionError(e);
                      }
                  }
              }
              """,
            """
              import lib.Json;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      try {
                          json.exists(content);
                      } catch (IllegalArgumentException e) {
                          throw new AssertionError("Bad path", e);
                      }
                      try {
                          json.doesNotExist(content);
                      } catch (IllegalArgumentException e) {
                          throw new AssertionError(e);
                      }
                      try {
                          json.exists(content);
                      } catch (IllegalStateException | IllegalArgumentException e) {
                          throw new AssertionError(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void resourceWhoseCloseCannotThrowIt() {
        rewriteRun(
          //language=java
          java(
            """
              import lib.Json;

              import java.io.IOException;
              import java.io.StringWriter;
              import java.text.ParseException;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      try (StringWriter copy = new StringWriter()) {
                          json.exists(content);
                          copy.write(content);
                      } catch (ParseException e) {
                          throw new AssertionError("Not JSON: " + content);
                      } catch (IOException e) {
                          throw new AssertionError(e);
                      }
                  }
              }
              """,
            """
              import lib.Json;

              import java.io.IOException;
              import java.io.StringWriter;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      try (StringWriter copy = new StringWriter()) {
                          json.exists(content);
                          copy.write(content);
                      } catch (IOException e) {
                          throw new AssertionError(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void localsDeclaredInTheTryKeepTheirScope() {
        rewriteRun(
          //language=java
          java(
            """
              import lib.Json;

              import java.text.ParseException;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      try {
                          String name = content.trim();
                          json.exists(name);
                      } catch (ParseException e) {
                          throw new AssertionError("Not JSON: " + content);
                      }
                      String name = content;
                      json.hashCode();
                  }
              }
              """,
            """
              import lib.Json;

              class Assertions {
                  void assertHasName(Json json, String content) {
                      {
                          String name = content.trim();
                          json.exists(name);
                      }
                      String name = content;
                      json.hashCode();
                  }
              }
              """
          )
        );
    }

    @Test
    void tryInPlaceOfAStatementBecomesABlock() {
        rewriteRun(
          //language=java
          java(
            """
              import lib.Json;

              import java.text.ParseException;

              class Assertions {
                  void assertHasName(Json json, String content, boolean check) {
                      if (check)
                          try {
                              json.exists(content);
                          } catch (ParseException e) {
                              throw new AssertionError("Not JSON: " + content);
                          }
                  }
              }
              """,
            """
              import lib.Json;

              class Assertions {
                  void assertHasName(Json json, String content, boolean check) {
                      if (check)
                          {
                              json.exists(content);
                          }
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void resourceCloseStillThrowsIt() {
            rewriteRun(
              spec -> spec.recipe(new RemoveCatchOfExceptionNoLongerThrown("lib.Schema populate(java.sql.Connection)", "java.sql.SQLException")),
              //language=java
              java(
                """
                  import lib.Schema;

                  import java.sql.Connection;
                  import java.sql.SQLException;
                  import java.util.Iterator;

                  class Loader {
                      void load(Iterator<Connection> connections, Schema schema) {
                          try (Connection connection = connections.next()) {
                              schema.populate(connection);
                          } catch (SQLException e) {
                              throw new IllegalStateException(e);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void anotherCallStillThrowsIt() {
            rewriteRun(
              //language=java
              java(
                """
                  import lib.Json;

                  import java.text.ParseException;
                  import java.text.SimpleDateFormat;

                  class Assertions {
                      void assertHasDate(Json json, String content) {
                          try {
                              json.exists(content);
                              new SimpleDateFormat("yyyy-MM-dd").parse(content);
                          } catch (ParseException e) {
                              throw new AssertionError("Not JSON: " + content);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void callThrowingSupertypeStillThrowsIt() {
            rewriteRun(
              //language=java
              java(
                """
                  import lib.Json;

                  import java.text.ParseException;
                  import java.util.concurrent.Callable;

                  class Assertions {
                      void assertHasName(Json json, String content, Callable<?> next) throws Exception {
                          try {
                              json.exists(content);
                              next.call();
                          } catch (ParseException e) {
                              throw new AssertionError("Not JSON: " + content);
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void statementsAfterTheTryReachedOnlyThroughTheCatch() {
            rewriteRun(
              //language=java
              java(
                """
                  import lib.Json;

                  import java.text.ParseException;

                  class Assertions {
                      String name(Json json, String content) {
                          try {
                              json.exists(content);
                              return content;
                          } catch (ParseException e) {
                          }
                          return null;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void catchOfSupertypeStays() {
            rewriteRun(
              //language=java
              java(
                """
                  import lib.Json;

                  class Assertions {
                      void assertHasName(Json json, String content) {
                          try {
                              json.exists(content);
                          } catch (Exception e) {
                              throw new AssertionError("Not JSON: " + content);
                          }
                      }
                  }
                  """
              )
            );
        }
    }
}
