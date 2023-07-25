/*
 * Copyright 2023 the original author or authors.
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
import static org.openrewrite.java.Assertions.version;

class ReplaceRecordWithClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceRecordWithClass());
    }

    @Test
    void simpleRecord() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model, int power) {
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;
                  private final int power;

                  public Vehicle(String model, int power) {
                      this.model = model;
                      this.power = power;
                  }

                  public String model() {
                      return model;
                  }

                  public int power() {
                      return power;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model) && power == other.power;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model, power);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + ", power=" + power + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void genericType() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(java.util.List<String> models) {
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final java.util.List<String> models;

                  public Vehicle(java.util.List<String> models) {
                      this.models = models;
                  }

                  public java.util.List<String> models() {
                      return models;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(models, other.models);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(models);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[models=" + models + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void genericRecord() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle<T>(T data) {
              }
              """),
            14));
    }

    @Test
    void canonicalConstructor() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model, int power) {
                  public Vehicle(String model, int power) {
                      this.model = model.toUpperCase();
                      this.power = power;
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;
                  private final int power;

                  public Vehicle(String model, int power) {
                      this.model = model.toUpperCase();
                      this.power = power;
                  }

                  public String model() {
                      return model;
                  }

                  public int power() {
                      return power;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model) && power == other.power;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model, power);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + ", power=" + power + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void nonCanonicalConstructor() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model, int power) {
                  public Vehicle(String model) {
                      this(model, 100);
                  }

                  public Vehicle() {
                      this("default", 100);
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;
                  private final int power;

                  public Vehicle(String model, int power) {
                      this.model = model;
                      this.power = power;
                  }

                  public Vehicle(String model) {
                      this(model, 100);
                  }

                  public Vehicle() {
                      this("default", 100);
                  }

                  public String model() {
                      return model;
                  }

                  public int power() {
                      return power;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model) && power == other.power;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model, power);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + ", power=" + power + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void compactConstructor() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              import java.util.Objects;

              public record Vehicle(String model, int power) {
                  public Vehicle {
                      Objects.requireNonNull(model);
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;
                  private final int power;

                  public Vehicle(String model, int power) {
                      Objects.requireNonNull(model);
                      this.model = model;
                      this.power = power;
                  }

                  public String model() {
                      return model;
                  }

                  public int power() {
                      return power;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model) && power == other.power;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model, power);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + ", power=" + power + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void staticField() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model) {
                  public static final String DEFAULT_MODEL = "default";
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  public static final String DEFAULT_MODEL = "default";

                  private final String model;

                  public Vehicle(String model) {
                      this.model = model;
                  }

                  public String model() {
                      return model;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void customMethod() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model) {
                  public boolean isDefault() {
                      return "default".equals(model);
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;

                  public Vehicle(String model) {
                      this.model = model;
                  }

                  public String model() {
                      return model;
                  }

                  public boolean isDefault() {
                      return "default".equals(model);
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void customGetter() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model) {
                  public String model() {
                      return model.toUpperCase();
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;

                  public Vehicle(String model) {
                      this.model = model;
                  }

                  public String model() {
                      return model.toUpperCase();
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void methodNamedAsGetter() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model) {
                  public String model(boolean upperCase) {
                      return upperCase ? model.toUpperCase() : model;
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;

                  public Vehicle(String model) {
                      this.model = model;
                  }

                  public String model() {
                      return model;
                  }

                  public String model(boolean upperCase) {
                      return upperCase ? model.toUpperCase() : model;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void customOverridenMethod() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public record Vehicle(String model) {
                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      return Objects.equals(model(), ((Vehicle) obj).model());
                  }

                  @Override
                  public int hashCode() {
                      return model.hashCode();
                  }

                  @Override
                  public String toString() {
                      return "Vehicle(" + model + ")";
                  }
              }
              """,
              """
              package com.example;

              public final class Vehicle {
                  private final String model;

                  public Vehicle(String model) {
                      this.model = model;
                  }

                  public String model() {
                      return model;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      return Objects.equals(model(), ((Vehicle) obj).model());
                  }

                  @Override
                  public int hashCode() {
                      return model.hashCode();
                  }

                  @Override
                  public String toString() {
                      return "Vehicle(" + model + ")";
                  }
              }
              """),
            14));
    }

    @Test
    void implementsInterface() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public interface Product {
                  String model();
              }

              public record Vehicle(String model) implements Product {
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public interface Product {
                  String model();
              }

              public final class Vehicle implements Product {
                  private final String model;

                  public Vehicle(String model) {
                      this.model = model;
                  }

                  @Override
                  public String model() {
                      return model;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void localRecord() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              public class SomeClass {
                  public void printRecord(String model) {
                      record Vehicle(String model) {}
                      System.out.println(new Vehicle(model));
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public class SomeClass {
                  public void printRecord(String model) {
                      final class Vehicle {
                          private final String model;

                          public Vehicle(String model) {
                              this.model = model;
                          }

                          public String model() {
                              return model;
                          }

                          @Override
                          public boolean equals(Object obj) {
                              if (this == obj) {
                                  return true;
                              }
                              if (obj == null || getClass() != obj.getClass()) {
                                  return false;
                              }
                              Vehicle other = (Vehicle) obj;
                              return Objects.equals(model, other.model);
                          }

                          @Override
                          public int hashCode() {
                              return Objects.hash(model);
                          }

                          @Override
                          public String toString() {
                              return "Vehicle[model=" + model + "]";
                          }
                      }
                      System.out.println(new Vehicle(model));
                  }
              }
              """),
            14));
    }

    @Test
    void comments() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              /**
               * The vehicle class.
               */
              public record Vehicle(String model) {
                  // Constructor
                  public Vehicle {
                      // Model is required
                      Objects.requireNonNull(model);
                  }

                  // Predefined getter
                  public String model() {
                      return model;
                  }
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              /**
               * The vehicle class.
               */
              public final class Vehicle {
                  private final String model;

                  // Constructor
                  public Vehicle(String model) {
                      // Model is required
                      Objects.requireNonNull(model);
                      this.model = model;
                  }

                  // Predefined getter
                  public String model() {
                      return model;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null || getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + "]";
                  }
              }
              """),
            14));
    }

}
